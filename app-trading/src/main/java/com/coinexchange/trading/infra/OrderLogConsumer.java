package com.coinexchange.trading.infra;

import com.coinexchange.trading.application.OrderProjectionService;
import com.coinexchange.trading.application.event.OrderPlacedEvent;
import com.coinexchange.trading.application.event.TradeExecutedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.coinexchange.trading.config.KafkaTopicConfig.ORDER_LOG_TOPIC;
import static com.coinexchange.trading.infra.OrderLogPublisher.*;

/**
 * 주문 durable 로그(Kafka)를 읽어 DB(사본)에 projection한다.
 *
 * DB 반영이 커밋된 뒤에 offset을 수동 커밋(ack)한다.
 * 반영 후 커밋 전에 크래시하면 재시작 시 그 이벤트부터 다시 읽어 재반영하는데,
 * projection이 dedup으로 멱등이라 중복 없이 이어진다. → 유실 없는 at-least-once.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLogConsumer {

    private final OrderProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = ORDER_LOG_TOPIC, groupId = "order-projection")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        String eventType = header(record);

        if (TYPE_ORDER_PLACED.equals(eventType)) {
            projectionService.applyOrderPlaced(
                    objectMapper.readValue(record.value(), OrderPlacedEvent.class));
        } else if (TYPE_TRADE_EXECUTED.equals(eventType)) {
            projectionService.applyTradeExecuted(
                    objectMapper.readValue(record.value(), TradeExecutedEvent.class));
        } else {
            log.warn("알 수 없는 이벤트 타입: {}", eventType);
        }

        ack.acknowledge();
    }

    private String header(ConsumerRecord<String, String> record) {
        var h = record.headers().lastHeader(HEADER_EVENT_TYPE);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}
