package com.coinexchange.trading.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class FundsClient {

    private final RestClient restClient;

    public FundsClient(@Value("${services.funds.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void debitKrw(Long userId, BigDecimal amount) {
        restClient.post()
                .uri("/internal/funds/krw/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KrwAmountRequest(userId, amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void debitCoin(Long userId, Long coinId, Long amount) {
        restClient.post()
                .uri("/internal/funds/coin/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CoinAmountRequest(userId, coinId, amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void settle(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw) {
        restClient.post()
                .uri("/internal/funds/settle")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SettleRequest(buyerId, sellerId, coinId, matchedAmount, totalKrw))
                .retrieve()
                .toBodilessEntity();
    }

    private record KrwAmountRequest(Long userId, BigDecimal amount) {}
    private record CoinAmountRequest(Long userId, Long coinId, Long amount) {}
    private record SettleRequest(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw) {}
}
