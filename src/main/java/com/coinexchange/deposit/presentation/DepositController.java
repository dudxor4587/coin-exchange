package com.coinexchange.deposit.presentation;

import com.coinexchange.auth.annotation.CurrentUserId;
import com.coinexchange.deposit.application.DepositService;
import com.coinexchange.deposit.presentation.dto.DepositRequest;
import com.coinexchange.deposit.application.dto.DepositResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @PostMapping("/deposits")
    public ResponseEntity<DepositResponse> depositRequest(@RequestBody DepositRequest depositRequest,
                                                          @CurrentUserId Long userId) {
        BigDecimal amount = depositRequest.amount();
        String bank = depositRequest.bank();
        String accountNumber = depositRequest.accountNumber();
        DepositResponse response = depositService.depositRequest(amount, bank, accountNumber, userId);

        log.info("입금 요청이 완료되었습니다. 사용자 ID: {}, 금액: {}, 은행: {}, 계좌번호: {}",
                userId, amount, bank, accountNumber);

        return ResponseEntity.ok(response);
    }
}
