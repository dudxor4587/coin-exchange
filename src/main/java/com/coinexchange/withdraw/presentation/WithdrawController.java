package com.coinexchange.withdraw.presentation;

import com.coinexchange.auth.annotation.CurrentUserId;
import com.coinexchange.withdraw.application.WithdrawService;
import com.coinexchange.withdraw.application.dto.WithdrawResponse;
import com.coinexchange.withdraw.presentation.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("/withdraws")
    public ResponseEntity<WithdrawResponse> withdrawRequest(@CurrentUserId Long userId,
                                                            @RequestBody WithdrawRequest withdrawRequest) {
        String bank = withdrawRequest.bank();
        String accountNumber = withdrawRequest.accountNumber();
        BigDecimal amount = withdrawRequest.amount();
        WithdrawResponse response = withdrawService.withdrawRequest(userId, bank, accountNumber, amount);

        log.info("출금 요청이 완료되었습니다. 사용자 ID: {}, 금액: {}, 은행: {}, 계좌번호: {}",
                userId, amount, bank, accountNumber);

        return ResponseEntity.ok(response);
    }
}
