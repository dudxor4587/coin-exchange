package com.coinexchange.admin.presentation;

import com.coinexchange.admin.application.AdminService;
import com.coinexchange.admin.presentation.dto.DepositApproveRequest;
import com.coinexchange.admin.presentation.dto.DepositRejectRequest;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/deposit/approve")
    public ResponseEntity<String> approveDeposit(@RequestBody DepositApproveRequest request) {
        Long depositId = request.depositId();
        adminService.approveDeposit(depositId);

        log.info("입금 요청이 승인되었습니다. 입금 요청 ID: {}", depositId);

        return ResponseEntity.ok("입금 요청이 승인되었습니다.");
    }

    @PostMapping("/deposit/reject")
    public ResponseEntity<String> rejectDeposit(@RequestBody DepositRejectRequest request) {
        Long depositId = request.depositId();
        String reason = request.reason();
        adminService.rejectDeposit(depositId, reason);

        log.info("입금 요청이 거부되었습니다. 입금 요청 ID : {}, 거절 사유 : {}", depositId, reason);

        return ResponseEntity.ok("입금 요청이 거부되었습니다.");
    }
}
