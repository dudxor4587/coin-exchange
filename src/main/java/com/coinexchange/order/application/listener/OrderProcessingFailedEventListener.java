package com.coinexchange.order.application.listener;

import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.event.OrderProcessingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingFailedEventListener {

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = ORDER_PROCESSING_FAILED_QUEUE)
    public void handleOrderProcessingFailed(OrderProcessingFailedEvent event) {
        log.error("주문 처리 실패 이벤트 수신: orderId={}, reason={}", event.orderId(), event.reason());
        try {
            orderService.handleOrderProcessingFailure(event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("주문 처리 실패 후 추가 작업 중 오류 발생: {}", e.getMessage());
            eventPublisher.publishEvent(new OrderProcessingFailedEvent(
                    event.orderId(),
                    "추가 작업 중 오류 발생: " + e.getMessage()
            ));
        }
    }
}
