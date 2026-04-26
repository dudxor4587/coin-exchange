package com.coinexchange.withdraw.admin.presentation;

import com.coinexchange.withdraw.admin.application.WithdrawAdminService;
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

    private final WithdrawAdminService withdrawAdminService;

    @PostMapping("/approve")
    public ResponseEntity<String> approve(@RequestBody WithdrawApproveRequest request) {
        Long withdrawId = request.withdrawId();
        withdrawAdminService.approve(withdrawId);

        log.info("출금 요청이 승인되었습니다. 출금 요청 ID: {}", withdrawId);

        return ResponseEntity.ok("출금 요청이 승인되었습니다.");
    }

    @PostMapping("/reject")
    public ResponseEntity<String> reject(@RequestBody WithdrawRejectRequest request) {
        Long withdrawId = request.withdrawId();
        String reason = request.reason();
        withdrawAdminService.reject(withdrawId, reason);

        log.info("출금 요청이 거부되었습니다. 출금 요청 ID: {}, 거절 사유: {}", withdrawId, reason);

        return ResponseEntity.ok("출금 요청이 거부되었습니다.");
    }
}
