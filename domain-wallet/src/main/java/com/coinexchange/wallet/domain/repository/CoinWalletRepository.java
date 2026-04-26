package com.coinexchange.wallet.domain.repository;

import com.coinexchange.wallet.domain.CoinWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoinWalletRepository extends JpaRepository<CoinWallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cw FROM CoinWallet cw WHERE cw.userId = :userId AND cw.coinId = :coinId")
    Optional<CoinWallet> findByUserIdAndCoinIdForUpdate(@Param("userId") Long userId, @Param("coinId") Long coinId);
}
