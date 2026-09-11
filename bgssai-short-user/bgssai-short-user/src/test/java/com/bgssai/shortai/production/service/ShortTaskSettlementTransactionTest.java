package com.bgssai.shortai.production.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.aimodel.AiModelResolveService;

class ShortTaskSettlementTransactionTest {
    JdbcTemplate db; ShortTaskSettlementService service; QuotaConsumeService quota;
    AiUsageRecordMapper usage; ShortAiTask task; AiModelConfig model;
    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1", "sa", "");
        db=new JdbcTemplate(ds);
        db.execute("CREATE TABLE task_state(id INT PRIMARY KEY, status VARCHAR(20))");
        db.execute("CREATE TABLE wallet(id INT PRIMARY KEY, balance INT)");
        db.execute("CREATE TABLE usage_log(id INT)");
        db.execute("CREATE TABLE work_cost(id INT PRIMARY KEY, cost INT)");
        db.update("INSERT INTO task_state VALUES(9, 'RUNNING')"); db.update("INSERT INTO wallet VALUES(1,100)"); db.update("INSERT INTO work_cost VALUES(1,0)");
        var tasks=mock(ShortAiTaskMapper.class); quota=mock(QuotaConsumeService.class);
        usage=mock(AiUsageRecordMapper.class); var works=mock(ShortSettlementMapper.class);
        when(tasks.updateByExampleSelective(any(),any())).thenAnswer(i -> db.update("UPDATE task_state SET status='SUCCEEDED' WHERE id=9 AND status='RUNNING'"));
        doAnswer(i -> { db.update("UPDATE wallet SET balance=balance-15 WHERE id=1"); return null; }).when(quota).consume("u-1",15,"task:t-1");
        when(usage.insertSelective(any())).thenAnswer(i -> db.update("INSERT INTO usage_log VALUES(1)"));
        when(works.addWorkCost("w-1","u-1",15)).thenAnswer(i -> db.update("UPDATE work_cost SET cost=cost+15 WHERE id=1"));
        var target=new ShortTaskSettlementService(tasks,quota,usage,works,mock(AiModelResolveService.class));
        var proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource()));
        service=(ShortTaskSettlementService)proxy.getProxy();
        task=new ShortAiTask(); task.setId(9L);task.setTaskId("t-1");task.setUserId("u-1");task.setWorkId("w-1");task.setCostCent(15);
        model=new AiModelConfig();
    }
    @Test void successCommitsOnceAndRepeatDoesNotCharge() {
        assertTrue(service.settle(task,model,1)); assertFalse(service.settle(task,model,1));
        assertEquals("SUCCEEDED",db.queryForObject("SELECT status FROM task_state",String.class));
        assertEquals(85,db.queryForObject("SELECT balance FROM wallet",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM usage_log",Integer.class));
        assertEquals(15,db.queryForObject("SELECT cost FROM work_cost",Integer.class));
        verify(quota).consume("u-1",15,"task:t-1");
    }
    @Test void failureAfterWalletDebitRollsBackStatusAndMoney() {
        doThrow(new IllegalStateException("ledger unavailable")).when(usage).insertSelective(any());
        assertThrows(IllegalStateException.class,()->service.settle(task,model,1));
        assertEquals("RUNNING",db.queryForObject("SELECT status FROM task_state",String.class));
        assertEquals(100,db.queryForObject("SELECT balance FROM wallet",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM usage_log",Integer.class));
        assertEquals(0,db.queryForObject("SELECT cost FROM work_cost",Integer.class));
    }
    @Test void cancelledTaskNeverCharges() {
        db.update("UPDATE task_state SET status='CANCELED'");
        assertFalse(service.settle(task,model,1)); verifyNoInteractions(quota,usage);
        assertEquals("CANCELED",db.queryForObject("SELECT status FROM task_state",String.class));
    }
}
