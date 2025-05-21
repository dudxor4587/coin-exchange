package com.coinexchange.admin.application.publisher;

import com.coinexchange.deposit.event.DepositApprovedEvent;

public interface DepositApprovedEventPublisher {

    void publish(DepositApprovedEvent event);
}
