package com.coinexchange.deposit.domain.repository;

import com.coinexchange.deposit.domain.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
}
