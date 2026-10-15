package com.bgssai.shortai.billing;
import com.bgssai.shortai.mapper.UsageReceiptMapper;
import com.github.pagehelper.PageHelper;
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
 private final UsageReceiptMapper mapper;
 private final ReceiptCredentialResolver credentials;
 private final TransactionTemplate tx;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
 private final JsonMapper json=JsonMapper.builder().build();
 public UsageReceiptService(UsageReceiptMapper mapper,ReceiptCredentialResolver credentials,PlatformTransactionManager manager){
  this.mapper=mapper;this.credentials=credentials;this.tx=new TransactionTemplate(manager);
  tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 public UsageReceiptRow begin(String endpoint,String key,String model){
  UsageScope scope=UsageScope.current();
  if(scope==null) scope=fallbackScope();
  if(scope==null || scope.userId()==null) throw new IllegalStateException("Model execution requires an owner");
  UsageReceiptRow row=new UsageReceiptRow();row.userId=scope.userId();row.workRef=scope.workRef();row.taskRef=scope.taskRef();
  row.endpoint=endpoint.replaceAll("/+$","");row.hub=URI.create(row.endpoint).getAuthority();
  row.keyFingerprint=fingerprint(key);row.model=model;row.requestId=UUID.randomUUID().toString();
  row.nextPollAt=System.currentTimeMillis()+60000;
  tx.executeWithoutResult(s->mapper.insertSelective(row));return row;
 }
 private UsageScope fallbackScope(){String user=com.bgssai.shortai.common.aop.UserContext.getUserId();
  return user==null?null:new UsageScope(user,null,"interactive");}
 public static String fingerprint(String key){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 public void refresh(UsageReceiptRow row,String key){
  if(row==null || row.reconciled || key==null || key.isBlank() || !fingerprint(key).equals(row.keyFingerprint)) return;
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
  row.receiptJson=body;row.reconciled=!"pending".equals(status);
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
 public List<Map<String,Object>> list(String userId,String workRef,String taskRef){
  if(userId==null || userId.isBlank()) throw new IllegalArgumentException("Owner required");
  UsageReceiptExample ex=new UsageReceiptExample();ex.userId=userId;ex.workRef=blankNull(workRef);ex.taskRef=blankNull(taskRef);
  PageHelper.startPage(1,100,false);
  List<UsageReceiptRow> rows=mapper.selectByExample(ex);
  List<Map<String,Object>> out=new ArrayList<>();
  for(UsageReceiptRow row:rows){
   Map<String,Object> value=new LinkedHashMap<>();value.put("hub",row.hub);value.put("request_id",row.requestId);
   value.put("model",row.model);value.put("work_ref",row.workRef);value.put("task_ref",row.taskRef);
   value.put("receipt",row.receiptJson==null ? pending(row) : json.readTree(row.receiptJson));out.add(value);
  }return out;
 }
 private Map<String,Object> pending(UsageReceiptRow row){
  Map<String,Object> r=new LinkedHashMap<>();r.put("request_id",row.requestId);r.put("billing_status","pending");
  for(String key:List.of("raw_tokens","billed_tokens","billing_multiplier","billing_unit","price_version","debit_fen","currency")) r.put(key,null);
  return r;
 }
 private static String blankNull(String s){return s==null||s.isBlank()?null:s;}
}
