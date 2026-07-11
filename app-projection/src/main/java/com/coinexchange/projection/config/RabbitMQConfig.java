package com.coinexchange.projection.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.coinexchange.common.config.RabbitMQChannels.NOTIFICATION_REQUESTED_EXCHANGE;

/**
 * 체결 알림 발행용. 컨슈머(notification)가 큐/바인딩을 선언하므로 여기선 익스체인지만 둔다.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange notificationRequestedExchange() {
        return new TopicExchange(NOTIFICATION_REQUESTED_EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
