package com.coinexchange.wallet.presentation;

import com.coinexchange.wallet.application.CoinWalletService;
import com.coinexchange.wallet.application.SettlementService;
import com.coinexchange.wallet.application.WalletService;
import com.coinexchange.wallet.presentation.dto.CoinAmountRequest;
import com.coinexchange.wallet.presentation.dto.KrwAmountRequest;
import com.coinexchange.wallet.presentation.dto.SettleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/funds")
@RequiredArgsConstructor
public class InternalFundsController {

    private final WalletService walletService;
    private final CoinWalletService coinWalletService;
    private final SettlementService settlementService;

    @PostMapping("/krw/credit")
    public ResponseEntity<Void> creditKrw(@RequestBody KrwAmountRequest req) {
        walletService.creditKrw(req.userId(), req.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/krw/debit")
    public ResponseEntity<Void> debitKrw(@RequestBody KrwAmountRequest req) {
        walletService.debitKrw(req.userId(), req.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/coin/credit")
    public ResponseEntity<Void> creditCoin(@RequestBody CoinAmountRequest req) {
        coinWalletService.creditCoin(req.userId(), req.coinId(), req.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/coin/debit")
    public ResponseEntity<Void> debitCoin(@RequestBody CoinAmountRequest req) {
        coinWalletService.debitCoin(req.userId(), req.coinId(), req.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settle")
    public ResponseEntity<Void> settle(@RequestBody SettleRequest req) {
        settlementService.settleMatch(req.buyerId(), req.sellerId(), req.coinId(),
                req.matchedAmount(), req.totalKrw());
        return ResponseEntity.ok().build();
    }
}
