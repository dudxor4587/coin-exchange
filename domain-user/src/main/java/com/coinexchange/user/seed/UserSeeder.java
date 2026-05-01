package com.coinexchange.user.seed;

import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("user seed 이미 존재 - 스킵");
            return;
        }

        userRepository.save(User.builder()
                .email("buyer@test.com")
                .password("test")
                .name("buyer")
                .phone("010-0000-0001")
                .role(User.Role.USER)
                .build());

        userRepository.save(User.builder()
                .email("seller@test.com")
                .password("test")
                .name("seller")
                .phone("010-0000-0002")
                .role(User.Role.USER)
                .build());

        log.info("user seed 완료: buyer/seller (예상 ID 1, 2)");
    }
}
