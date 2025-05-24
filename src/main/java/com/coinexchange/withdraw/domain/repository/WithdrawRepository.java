package com.coinexchange.withdraw.domain.repository;

import com.coinexchange.withdraw.domain.Withdraw;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawRepository extends JpaRepository<Withdraw, Long> {
}
