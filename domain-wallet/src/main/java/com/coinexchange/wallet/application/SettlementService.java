package com.coinexchange.wallet.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final WalletService walletService;
    private final CoinWalletService coinWalletService;

    @Transactional
    public void settleMatch(Long buyerId, Long sellerId, Long coinId, Long matchedAmount, BigDecimal totalKrw) {
        coinWalletService.creditCoin(buyerId, coinId, matchedAmount);
        walletService.creditKrw(sellerId, totalKrw);
        log.info("체결 정산 완료: buyer={}, seller={}, coinId={}, amount={}, krw={}",
                buyerId, sellerId, coinId, matchedAmount, totalKrw);
    }
}
