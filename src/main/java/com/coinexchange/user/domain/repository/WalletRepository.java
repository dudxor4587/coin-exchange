package com.coinexchange.user.domain.repository;

import com.coinexchange.user.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndCurrency(Long aLong, Wallet.Currency currency);
}
