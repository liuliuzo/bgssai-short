package com.bgssai.shortai.billing;
import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.mapper.UsageReceiptMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class UsageReceiptService {
 private static final Logger log = LoggerFactory.getLogger(UsageReceiptService.class);
 private final UsageReceiptMapper mapper;
 private final ReceiptCredentialResolver credentials;
 private final TransactionTemplate tx;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
 private final JsonMapper json=JsonMapper.builder().build();
 private final ObjectMapper jackson2=new ObjectMapper();
 public UsageReceiptService(UsageReceiptMapper mapper,ReceiptCredentialResolver credentials,PlatformTransactionManager manager){
  this.mapper=mapper;this.credentials=credentials;this.tx=new TransactionTemplate(manager);
  tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 /**
  * Persist a receipt row before calling the model gateway.
  * Schema / DB failures return an in-memory row so generation is not blocked when the
  * upgrade table is not yet applied; reconciliation simply skips those rows.
  */
 public UsageReceiptRow begin(String endpoint,String key,String model){
  UsageScope scope=UsageScope.current();
  if(scope==null) scope=fallbackScope();
  if(scope==null || scope.userId()==null) throw new IllegalStateException("Model execution requires an owner");
  UsageReceiptRow row=new UsageReceiptRow();row.userId=scope.userId();row.workRef=scope.workRef();row.taskRef=scope.taskRef();
  row.endpoint=endpoint.replaceAll("/+$","");row.hub=URI.create(row.endpoint).getAuthority();
  row.keyFingerprint=fingerprint(key);row.model=model;row.requestId=UUID.randomUUID().toString();
  row.nextPollAt=System.currentTimeMillis()+60000;
  try{
   tx.executeWithoutResult(s->mapper.insertSelective(row));
  }catch(RuntimeException ex){
   log.warn("usage receipt begin skipped (table/datasource unavailable); generation continues requestId={} cause={}",
     row.requestId, summarize(ex));
   row.id=null;
  }
  return row;
 }
 private UsageScope fallbackScope(){String user=com.bgssai.shortai.common.aop.UserContext.getUserId();
  return user==null?null:new UsageScope(user,null,"interactive");}
 public static String fingerprint(String key){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 public void refresh(UsageReceiptRow row,String key){
  if(row==null || row.id==null || row.reconciled || key==null || key.isBlank() || !fingerprint(key).equals(row.keyFingerprint)) return;
  try{
   HttpRequest request=HttpRequest.newBuilder(URI.create(row.endpoint+"/v1/usage/"+row.requestId))
    .timeout(Duration.ofSeconds(5)).header("Authorization","Bearer "+key).GET().build();
   HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()==200) accept(row,response.body());
  }catch(InterruptedException ex){Thread.currentThread().interrupt();}
  catch(Exception ignored){ /* durable pending survives upstream unavailability */ }
  row.nextPollAt=System.currentTimeMillis()+(row.receiptJson==null?300000:60000);
  try{tx.executeWithoutResult(s->mapper.updateByPrimaryKeySelective(row));}catch(RuntimeException ignored){/* retry from durable pending */}
 }
 void accept(UsageReceiptRow row,String body){
  var value=json.readTree(body);
  if(!row.requestId.equals(value.path("request_id").asString(""))) return;
  String status=value.path("billing_status").asString("pending");
  if(!Set.of("pending","settled","unpaid","unpriced","reversed").contains(status)) return;
  row.receiptJson=body;row.reconciled=!"pending".equals(status) && value.path("billed_tokens").isIntegralNumber() && value.path("billed_tokens").asLong(-1)>=0;
 }
 @Scheduled(fixedDelay=60000)
 public void recoverPending(){
  try{
   UsageReceiptExample ex=new UsageReceiptExample();ex.pending=true;ex.dueBefore=System.currentTimeMillis();
   PageHelper.startPage(1,50,false);
   List<UsageReceiptRow> rows=mapper.selectByExample(ex);
   for(UsageReceiptRow row:rows){
    String key=credentials.resolve(row);
    if(key==null || key.isBlank()){
     row.nextPollAt=System.currentTimeMillis()+300000;tx.executeWithoutResult(s->mapper.updateByPrimaryKeySelective(row));
    }else refresh(row,key);
   }
  }catch(RuntimeException ignored){/* startup/migration unavailable: next scheduler tick retries */}
 }
 /**
  * Read-only receipt list. Missing table / bad JSON / Jackson mismatches must not 500 the SPA.
  * Receipt payloads are plain Maps so Spring MVC Jackson 2 can serialize them.
  */
 public List<Map<String,Object>> list(String userId,String workRef,String taskRef){
  if(userId==null || userId.isBlank()) throw new IllegalArgumentException("Owner required");
  try{
   UsageReceiptExample ex=new UsageReceiptExample();ex.userId=userId;ex.workRef=blankNull(workRef);ex.taskRef=blankNull(taskRef);
   PageHelper.startPage(1,100,false);
   List<UsageReceiptRow> rows=mapper.selectByExample(ex);
   List<Map<String,Object>> out=new ArrayList<>();
   for(UsageReceiptRow row:rows){
    Map<String,Object> value=new LinkedHashMap<>();value.put("hub",userHub(row.hub));value.put("request_id",row.requestId);
    ProductModelAlias.Face face=ProductModelAlias.forStored(null,row.model);
    value.put("model",face.code());value.put("display_name",face.name());
    value.put("work_ref",row.workRef);value.put("task_ref",row.taskRef);
    Map<String,Object> receipt=parseReceiptMap(row);hideReceiptModel(receipt);
    value.put("receipt",receipt);out.add(value);
   }
   return out;
  }catch(RuntimeException ex){
   log.warn("usage receipt list degraded to empty userId={} cause={}", userId, summarize(ex));
   return List.of();
  }
 }
 private Map<String,Object> parseReceiptMap(UsageReceiptRow row){
  if(row.receiptJson==null || row.receiptJson.isBlank()) return pending(row);
  try{
   return jackson2.readValue(row.receiptJson, new TypeReference<LinkedHashMap<String,Object>>() {});
  }catch(Exception ex){
   log.warn("usage receipt json parse failed requestId={}", row.requestId);
   return pending(row);
  }
 }
 private static void hideReceiptModel(Map<String,Object> receipt){
  if(receipt==null) return;
  Object model=receipt.get("model");
  if(model instanceof String text && ProductModelAlias.leaks(text)){
   receipt.put("model",ProductModelAlias.forStored(null,text).code());
  }
 }
 private Map<String,Object> pending(UsageReceiptRow row){
  Map<String,Object> r=new LinkedHashMap<>();r.put("request_id",row.requestId);r.put("billing_status","pending");
  for(String key:List.of("raw_tokens","billed_tokens","billing_multiplier","billing_unit","price_version","debit_fen","currency")) r.put(key,null);
  return r;
 }
 /** User lists only say the domestic region. The gateway host stays on the row. */
 private static String userHub(String hub){
  return hub == null || !hub.isBlank() ? "cn" : "cn";
 }
 private static String blankNull(String s){return s==null||s.isBlank()?null:s;}
 private static String summarize(Throwable ex){
  StringBuilder sb=new StringBuilder();
  Throwable cur=ex;int depth=0;
  while(cur!=null && depth<3){
   if(depth>0) sb.append(" | ");
   sb.append(cur.getClass().getSimpleName());
   if(cur.getMessage()!=null && !cur.getMessage().isBlank()) sb.append(": ").append(cur.getMessage());
   cur=cur.getCause();depth++;
  }
  return sb.toString();
 }
}
