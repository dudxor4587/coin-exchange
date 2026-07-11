package com.coinexchange.trading.infra;

import com.coinexchange.trading.application.event.OrderPlacedEvent;
import com.coinexchange.trading.application.event.TradeExecutedEvent;
import com.coinexchange.trading.config.KafkaTopicConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * 주문 durable 로그(Kafka)에 이벤트를 동기로 append한다.
 *
 * send().get()으로 브로커의 저장 확인(ack)을 기다린 뒤 반환한다.
 * 이 append가 "주문/체결이 일어났다"의 commit 지점이다 — 여기서 돌아오면
 * 프로세스가 크래시해도 로그에 남아 있어, 재시작한 컨슈머가 결국 DB에 반영한다.
 * 동기 대기라 hot path에 Kafka 왕복 지연이 더해진다. 내구성의 비용이다.
 *
 * eventType 헤더로 컨슈머가 이벤트 종류를 분기한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLogPublisher {

    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String TYPE_ORDER_PLACED = "ORDER_PLACED";
    public static final String TYPE_TRADE_EXECUTED = "TRADE_EXECUTED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void appendOrderPlaced(OrderPlacedEvent event) {
        append(TYPE_ORDER_PLACED, event);
    }

    public void appendTradeExecuted(TradeExecutedEvent event) {
        append(TYPE_TRADE_EXECUTED, event);
    }

    private void append(String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaTopicConfig.ORDER_LOG_TOPIC, null, null, json);
            record.headers().add(new RecordHeader(HEADER_EVENT_TYPE,
                    eventType.getBytes(StandardCharsets.UTF_8)));

            // 동기 append — 브로커 저장 확인까지 대기
            kafkaTemplate.send(record).get();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("주문 로그 직렬화 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("주문 로그 append 중단", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("주문 로그 append 실패", e);
        }
    }
}
