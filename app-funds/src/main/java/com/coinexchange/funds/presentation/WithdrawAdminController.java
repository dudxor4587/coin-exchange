package com.coinexchange.funds.presentation;

import com.coinexchange.funds.application.WithdrawApprovalService;
import com.coinexchange.withdraw.admin.presentation.dto.WithdrawApproveRequest;
import com.coinexchange.withdraw.admin.presentation.dto.WithdrawRejectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/withdraw")
@RequiredArgsConstructor
@Slf4j
public class WithdrawAdminController {

    private final WithdrawApprovalService withdrawApprovalService;

    @PostMapping("/approve")
    public ResponseEntity<String> approve(@RequestBody WithdrawApproveRequest request) {
        withdrawApprovalService.approve(request.withdrawId());
        return ResponseEntity.ok("출금 요청이 승인되었습니다.");
    }

    @PostMapping("/reject")
    public ResponseEntity<String> reject(@RequestBody WithdrawRejectRequest request) {
        withdrawApprovalService.reject(request.withdrawId(), request.reason());
        return ResponseEntity.ok("출금 요청이 거부되었습니다.");
    }
}
