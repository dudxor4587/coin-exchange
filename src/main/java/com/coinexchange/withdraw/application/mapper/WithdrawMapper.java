package com.coinexchange.withdraw.application.mapper;

import com.coinexchange.withdraw.application.dto.WithdrawResponse;
import com.coinexchange.withdraw.domain.Withdraw;

public class WithdrawMapper {

    public static WithdrawResponse toResponse(Withdraw withdraw) {
        return new WithdrawResponse(
                withdraw.getStatus().name(),
                withdraw.getAmount(),
                "출금 요청이 접수되었습니다. 출금이 확인되면 상태가 변경됩니다."
        );
    }
}
