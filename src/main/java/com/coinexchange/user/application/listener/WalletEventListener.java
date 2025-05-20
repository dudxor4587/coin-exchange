package com.coinexchange.user.application.listener;

import com.coinexchange.deposit.event.DepositApprovedEvent;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.Wallet;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.domain.repository.WalletRepository;
import com.coinexchange.user.exception.UserException;

import static com.coinexchange.common.config.RabbitMQConfig.DEPOSIT_QUEUE;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @RabbitListener(queues = DEPOSIT_QUEUE)
    public void handleDepositApproved(DepositApprovedEvent event) {
        log.info("입금 승인 이벤트 수신: userId={}, amount={}", event.userId(), event.amount());
        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Wallet wallet = walletRepository.findByUserIdAndCurrency(event.userId(), Wallet.Currency.KRW)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .currency(Wallet.Currency.KRW)
                        .build());

        wallet.increaseBalance(event.amount());

        walletRepository.save(wallet);
        log.info("지갑 잔액 갱신 완료: userId={}, 변경된 잔액={}", event.userId(), wallet.getBalance());
    }
}
