package com.bgssai.shortai.billing;
import com.bgssai.shortai.mapper.UsageReceiptMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

class UsageReceiptServiceTest {
 UsageReceiptMapper mapper; UsageReceiptService service;DataSourceTransactionManager manager;HttpServer server;String endpoint;
 AtomicInteger calls=new AtomicInteger();String status="pending";String overrideId;
 @BeforeEach void setup() throws Exception {
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
  new JdbcTemplate(ds).execute("CREATE TABLE tokenhub_usage_receipt(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id VARCHAR(64),work_ref VARCHAR(128),task_ref VARCHAR(255),hub VARCHAR(255),endpoint VARCHAR(255),key_fingerprint VARCHAR(64),model VARCHAR(255),request_id VARCHAR(64),receipt_json CLOB,reconciled BOOLEAN DEFAULT FALSE,next_poll_at BIGINT,del_flag INT DEFAULT 0,creator VARCHAR(64),modifier VARCHAR(64),gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(hub,request_id))");
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new ClassPathResource("mybatis/mapper/UsageReceiptMapper.xml"));factory.setPlugins(new com.github.pagehelper.PageInterceptor());
  mapper=new SqlSessionTemplate(factory.getObject()).getMapper(UsageReceiptMapper.class);manager=new DataSourceTransactionManager(ds);
  service=new UsageReceiptService(mapper,row->"original-key",manager);
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/v1/usage/",exchange->{
   calls.incrementAndGet();assertEquals("Bearer original-key",exchange.getRequestHeaders().getFirst("Authorization"));
   String id=overrideId==null?exchange.getRequestURI().getPath().substring("/v1/usage/".length()):overrideId;
   byte[] body=("{\"request_id\":\""+id+"\",\"billing_status\":\""+status+"\",\"raw_tokens\":100,\"billed_tokens\":150,\"billing_multiplier\":1.5,\"debit_fen\":0,\"currency\":\"USD\"}").getBytes(StandardCharsets.UTF_8);
   exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
  });server.start();endpoint="http://127.0.0.1:"+server.getAddress().getPort();UsageScope.set("owner","work","task");
 }
 @AfterEach void clean(){UsageScope.clear();server.stop(0);com.github.pagehelper.PageHelper.clearPage();}
 @Test void dispatchRecordSurvivesOuterRollbackAndRestartRecoversWithoutGeneration(){
  UsageReceiptRow[] stored=new UsageReceiptRow[1];
  new TransactionTemplate(manager).executeWithoutResult(tx->{stored[0]=service.begin(endpoint,"original-key","model");tx.setRollbackOnly();});
  var row=mapper.selectByPrimaryKey(stored[0].id);assertNotNull(row);assertNotEquals("original-key",row.keyFingerprint);
  row.nextPollAt=0;mapper.updateByPrimaryKeySelective(row);status="unpriced";
  new UsageReceiptService(mapper,r->"original-key",manager).recoverPending();
  var done=mapper.selectByPrimaryKey(row.id);assertTrue(done.reconciled);assertTrue(done.receiptJson.contains("150"));
  service.recoverPending();assertEquals(1,calls.get());
 }
 @Test void pendingIsDurableAndOwnerCannotReadAnotherUsersRecords(){
  var row=service.begin(endpoint,"original-key","model");service.refresh(row,"original-key");
  assertFalse(mapper.selectByPrimaryKey(row.id).reconciled);
  assertEquals(1,service.list("owner","work",null).size());assertTrue(service.list("other","work",null).isEmpty());
  String output=service.list("owner",null,null).toString();assertFalse(output.contains("key_fingerprint"));assertFalse(output.contains("original-key"));
 }
 @Test void rotatedCredentialAndWrongRequestReceiptNeverSettle(){
  var row=service.begin(endpoint,"original-key","model");service.refresh(row,"rotated-key");assertEquals(0,calls.get());
  overrideId="wrong-id";status="settled";service.refresh(row,"original-key");assertFalse(mapper.selectByPrimaryKey(row.id).reconciled);
 }
 @Test void terminalReceiptCannotBeOverwrittenByStalePendingWorker(){
  var row=service.begin(endpoint,"original-key","model");var stale=mapper.selectByPrimaryKey(row.id);
  status="settled";service.refresh(row,"original-key");status="pending";service.refresh(stale,"original-key");
  assertTrue(mapper.selectByPrimaryKey(row.id).reconciled);assertTrue(mapper.selectByPrimaryKey(row.id).receiptJson.contains("settled"));
 }
 @Test void everyActualInvocationGetsDistinctIdAndUnknownReceiptHasNoFakeDebit(){
  var first=service.begin(endpoint,"original-key","model");var second=service.begin(endpoint,"original-key","model");
  assertNotEquals(first.requestId,second.requestId);
  var receipt=(java.util.Map<?,?>)service.list("owner","work","task").getFirst().get("receipt");
  assertEquals("pending",receipt.get("billing_status"));assertNull(receipt.get("billed_tokens"));assertNull(receipt.get("debit_fen"));
 }
 @Test void reconciliationWaitsForBothQuotaAndMoneyStatus(){
  var row=service.begin(endpoint,"original-key","model");String prefix="{\"request_id\":\""+row.requestId+"\",\"billing_status\":\"";
  service.accept(row,prefix+"pending\",\"billed_tokens\":150}");assertFalse(row.reconciled);
  service.accept(row,prefix+"settled\",\"debit_fen\":5}");assertFalse(row.reconciled);
  service.accept(row,prefix+"settled\",\"billed_tokens\":0,\"debit_fen\":0}");assertTrue(row.reconciled);
 }
}
