package com.bgssai.shortai.quota.service;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.bgssai.shortai.mapper.ShortDebitOutboxMapper;
import com.bgssai.shortai.centralledger.service.CentralLedgerService;
@Service
public class ShortDebitOutbox {
    private final ShortDebitOutboxMapper rows;
    private final CentralLedgerService ledger;
    public ShortDebitOutbox(ShortDebitOutboxMapper rows, CentralLedgerService ledger) { this.rows=rows; this.ledger=ledger; }
    public void enqueue(String id, String userId, int amount) { rows.enqueue(id, userId, amount); }
    @Scheduled(fixedDelay = 15000)
    public void deliver() {
        for (var row : rows.pending(new com.bgssai.shortai.model.ShortDebitOutboxExample(50))) {
            if (rows.claim(row.getOutboxId()) != 1) continue;
            try {
                if (ledger.consume(row.getWebsiteUserId(), row.getAmount(), row.getUsageKind(), row.getOutboxId(), "short task debit")) {
                    rows.acknowledge(row.getOutboxId()); continue;
                }
            } catch (RuntimeException failure) { /* The durable row remains pending. */ }
            int attempts = row.getAttempts() == null ? 0 : row.getAttempts();
            rows.retry(row.getOutboxId(), Math.min(300, 5 * (1 << Math.min(6, attempts))));
        }
    }
}
