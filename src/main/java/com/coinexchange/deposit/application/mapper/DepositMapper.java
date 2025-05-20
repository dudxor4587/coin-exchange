package com.coinexchange.deposit.application.mapper;

import com.coinexchange.deposit.application.dto.DepositResponse;
import com.coinexchange.deposit.domain.Deposit;

public class DepositMapper {

    public static DepositResponse toResponse(Deposit deposit) {
        return new DepositResponse(
                deposit.getStatus().name(),
                deposit.getAmount(),
                "입금 요청이 접수되었습니다. 입금이 확인되면 상태가 변경됩니다."
        );
    }
}
