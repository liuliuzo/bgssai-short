package com.bgssai.shortai.quota.service;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import com.bgssai.shortai.mapper.ShortDebitOutboxMapper;
import com.bgssai.shortai.centralledger.service.CentralLedgerService;
class ShortDebitOutboxTest {
    private static com.bgssai.shortai.model.ShortDebitOutboxEntry entry(String id, String user, int amount, String kind, int attempts) {
        var row = new com.bgssai.shortai.model.ShortDebitOutboxEntry();
        row.setOutboxId(id); row.setWebsiteUserId(user); row.setAmount(amount); row.setUsageKind(kind); row.setAttempts(attempts);
        return row;
    }
    @Test void retryUsesSameIdempotencyKeyAfterUnacknowledgedDelivery() {
        var rows=mock(ShortDebitOutboxMapper.class);var ledger=mock(CentralLedgerService.class);
        var row=entry("task:t1","central-u1",15,"quota_cent",0);
        when(rows.pending(any(com.bgssai.shortai.model.ShortDebitOutboxExample.class))).thenReturn(java.util.List.of(row));when(rows.claim(row.getOutboxId())).thenReturn(1);
        when(ledger.consume(row.getWebsiteUserId(),15,row.getUsageKind(),row.getOutboxId(),"short task debit")).thenReturn(true);
        when(rows.acknowledge(row.getOutboxId())).thenThrow(new IllegalStateException("connection lost")).thenReturn(1);
        var outbox=new ShortDebitOutbox(rows,ledger);outbox.deliver();outbox.deliver();
        verify(ledger,times(2)).consume("central-u1",15,"quota_cent","task:t1","short task debit");
        verify(rows).retry("task:t1",5);verify(rows,times(2)).acknowledge("task:t1");
    }
    @Test void rejectedOrBusyLedgerRemainsPending() {
        var rows=mock(ShortDebitOutboxMapper.class);var ledger=mock(CentralLedgerService.class);
        when(rows.pending(any(com.bgssai.shortai.model.ShortDebitOutboxExample.class))).thenReturn(java.util.List.of(entry("task:t1","u1",15,"quota_cent",2)));
        when(rows.claim("task:t1")).thenReturn(1);
        new ShortDebitOutbox(rows,ledger).deliver();
        verify(rows).retry("task:t1",20);verify(rows,never()).acknowledge("task:t1");
    }
}
