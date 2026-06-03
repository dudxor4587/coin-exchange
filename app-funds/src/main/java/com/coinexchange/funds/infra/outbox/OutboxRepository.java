package com.coinexchange.funds.infra.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select m from OutboxMessage m where m.status = com.coinexchange.funds.infra.outbox.OutboxMessage.Status.PENDING order by m.id asc")
    List<OutboxMessage> findPendingForUpdate(Pageable pageable);
}
