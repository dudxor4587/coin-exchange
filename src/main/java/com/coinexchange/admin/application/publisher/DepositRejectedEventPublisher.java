package com.coinexchange.admin.application.publisher;

import com.coinexchange.deposit.event.DepositRejectedEvent;

public interface DepositRejectedEventPublisher {
    void publish(DepositRejectedEvent event);
}
