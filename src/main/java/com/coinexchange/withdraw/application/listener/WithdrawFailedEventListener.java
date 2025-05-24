package com.coinexchange.withdraw.application.listener;

import com.coinexchange.withdraw.application.WithdrawService;
import com.coinexchange.withdraw.event.WithdrawFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.WITHDRAW_FAILURE_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawFailedEventListener {

    private final WithdrawService withdrawService;

    @RabbitListener(queues = WITHDRAW_FAILURE_QUEUE)
    public void handleWithdrawApprovalFailed(WithdrawFailedEvent event) {
        log.info("출금 실패 이벤트 수신: withdrawId={}, reason={}", event.withdrawId(), event.reason());
        withdrawService.handleWithdrawFailure(event.withdrawId(), event.reason());
    }
}
