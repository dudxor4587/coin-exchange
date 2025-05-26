package com.coinexchange.notification.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSender notificationSender;

    public void sendDepositRejectionNotification(Long userId, String message) {
        message = "거래소에 요청하신 입금이 거절되었습니다. 사유: " + message;
        notificationSender.send(userId, message);
    }

    public void sendDepositNotification(Long userId, BigDecimal amount) {
        String message = "거래소에 요청하신 입금 처리가 완료되었습니다. 입금액: " + amount;
        notificationSender.send(userId, message);
    }

    public void sendWithdrawNotification(Long userId, BigDecimal amount) {
        String message = "거래소에 요청하신 출금 처리가 완료되었습니다. 출금액: " + amount;
        notificationSender.send(userId, message);
    }

    public void sendWithdrawRejectionNotification(Long userId, String message) {
        message = "거래소에 요청하신 출금이 거절되었습니다. 사유: " + message;
        notificationSender.send(userId, message);
    }

    public void sendOrderNotification(Long userId, BigDecimal lockedFunds) {
        String message = "거래소에 요청하신 주문이 완료되었습니다. 주문 금액: " + lockedFunds;
        notificationSender.send(userId, message);
    }

    public void sendOrderFailureNotification(Long userId, String reason) {
        String message = "거래소에 요청하신 주문이 실패하였습니다. 사유: " + reason;
        notificationSender.send(userId, message);
    }
}
