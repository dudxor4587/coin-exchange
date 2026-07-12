package com.coinexchange.trading.infra;

import com.coinexchange.trading.exception.FundsClientException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static com.coinexchange.trading.exception.FundsClientExceptionType.FUNDS_UNAVAILABLE;

/**
 * funds-service로의 동기 RPC.
 *
 * 두 겹으로 연쇄붕괴를 막는다.
 * 1. 타임아웃 — funds가 응답 안 하면 무한 대기가 아니라 2초 만에 실패한다.
 * 2. 회로차단기(name="funds") — 실패가 쌓이면 funds 호출 자체를 끊고(open) 즉시 fallback으로 빠진다.
 *    워커 스레드가 funds 대기로 소진되는 것을 막아 trading을 살린다.
 *
 * funds의 4xx(잔액 부족 등 업무 오류)는 funds가 살아있다는 뜻이므로 회로에 카운트하지 않고
 * 원래 예외 그대로 전파한다(application.yml의 ignore-exceptions). 그 외(타임아웃/연결실패/5xx/회로open)만
 * FUNDS_UNAVAILABLE(503)로 바꿔 fail-fast 한다.
 */
@Component
public class FundsClient {

    private static final String CB = "funds";

    private final RestClient restClient;

    // 주입받은 RestClient.Builder는 Spring Boot가 ObservationRegistry로 계측해 둔 것이라,
    // 이걸 쓰면 trading→funds 호출에 trace 컨텍스트가 헤더로 전파된다(직접 builder()로 만들면 전파 안 됨).
    public FundsClient(@Value("${services.funds.base-url}") String baseUrl, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "debitKrwFallback")
    public void debitKrw(Long userId, BigDecimal amount) {
        restClient.post()
                .uri("/internal/funds/krw/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KrwAmountRequest(userId, amount))
                .retrieve()
                .toBodilessEntity();
    }

    void debitKrwFallback(Long userId, BigDecimal amount, Throwable t) {
        throw toException(t);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "debitCoinFallback")
    public void debitCoin(Long userId, Long coinId, Long amount) {
        restClient.post()
                .uri("/internal/funds/coin/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoinAmountRequest(userId, coinId, amount))
                .retrieve()
                .toBodilessEntity();
    }

    void debitCoinFallback(Long userId, Long coinId, Long amount, Throwable t) {
        throw toException(t);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "settleFallback")
    public void settle(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw) {
        restClient.post()
                .uri("/internal/funds/settle")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SettleRequest(buyerId, sellerId, coinId, matchedAmount, totalKrw))
                .retrieve()
                .toBodilessEntity();
    }

    void settleFallback(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw, Throwable t) {
        throw toException(t);
    }

    // 4xx(업무 오류)는 원래대로 전파, 그 외 가용성 실패만 503으로.
    private RuntimeException toException(Throwable t) {
        if (t instanceof HttpClientErrorException clientError) {
            return clientError;
        }
        return new FundsClientException(FUNDS_UNAVAILABLE);
    }

    private record KrwAmountRequest(Long userId, BigDecimal amount) {}
    private record CoinAmountRequest(Long userId, Long coinId, Long amount) {}
    private record SettleRequest(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw) {}
}
