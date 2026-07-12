package com.coinexchange.common.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class TracingConfig {

    /**
     * /actuator/** (헬스체크·프로메테우스 스크랩)은 트레이싱에서 제외한다.
     * 5초마다 도는 헬스체크 trace가 Zipkin을 도배해 실제 요청 trace를 덮는 걸 막는다.
     */
    @Bean
    ObservationPredicate noActuatorTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext ctx) {
                return !ctx.getCarrier().getRequestURI().startsWith("/actuator");
            }
            return true;
        };
    }
}
