package com.coinexchange.funds.infra.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from OutboxMessage m where m.status = com.coinexchange.funds.infra.outbox.OutboxMessage.Status.PENDING order by m.id asc")
    List<OutboxMessage> findPendingForUpdate(Pageable pageable);
}
