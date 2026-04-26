package com.coinexchange.infra.notification.application;

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

    public void sendBuyOrderNotification(Long userId, BigDecimal lockedFunds) {
        String message = "거래소에 요청하신 매수 주문이 완료되었습니다. 주문 금액: " + lockedFunds;
        notificationSender.send(userId, message);
    }

    public void sendSellOrderNotification(Long userId, Long amount) {
        String message = "거래소에 요청하신 매도 주문이 완료되었습니다. 주문 수량: " + amount;
        notificationSender.send(userId, message);
    }

    public void sendOrderFailureNotification(Long userId, String reason) {
        String message = "거래소에 요청하신 주문이 실패하였습니다. 사유: " + reason;
        notificationSender.send(userId, message);
    }

    public void sendOrderMatchNotification(Long buyUserId, Long sellUserId, Long matchedAmount, Long coinId, BigDecimal price) {
        String buyMessage = String.format("매수 완료: 매수자 ID: %d, 매칭 금액: %d, 코인 ID: %d, 가격: %s",
                buyUserId, matchedAmount, coinId, price);
        String sellMessage = String.format("매칭 완료: 매도자 ID: %d, 매칭 금액: %d, 코인 ID: %d, 가격: %s",
                sellUserId, matchedAmount, coinId, price);

        notificationSender.send(buyUserId, buyMessage);
        notificationSender.send(sellUserId, sellMessage);
    }

    public void sendBuyOrderCompletionNotification(Long userId, Long coinId, Long amount) {
        String message = String.format("매수 주문 완료: 사용자 ID: %d, 대상 코인 ID : %d, 주문 수량: %d", userId, coinId, amount);
        notificationSender.send(userId, message);
    }

    public void sendSellOrderCompletionNotification(Long userId, Long coinId, Long amount) {
        String message = String.format("매도 주문 완료: 사용자 ID: %d, 대상 코인 ID : %d, 주문 수량: %d", userId, coinId, amount);
        notificationSender.send(userId, message);
    }

    public void sendBuyOrderFillNotification(Long userId, Long coinId, Long amount) {
        String message = String.format("매수 주문 체결 완료: 사용자 ID: %d, 대상 코인 ID : %d, 체결 수량: %d", userId, coinId, amount);
        notificationSender.send(userId, message);
    }

    public void sendSellOrderFillNotification(Long userId, BigDecimal price) {
        String message = String.format("매도 주문 체결 완료: 사용자 ID: %d, 체결 금액: %s", userId, price);
        notificationSender.send(userId, message);
    }
}
