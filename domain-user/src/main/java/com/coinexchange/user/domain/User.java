package com.coinexchange.user.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.user.exception.UserException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.coinexchange.user.exception.UserExceptionType.PASSWORD_MISMATCH;

@Entity
@Getter
@NoArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER, ADMIN
    }

    public void isMatchPassword(String password, PasswordEncoder passwordEncoder) {
        if(!passwordEncoder.matches(password, this.password)) {
            throw new UserException(PASSWORD_MISMATCH);
        }
    }

    @Builder
    public User(String email, String password, String name, String phone, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }
}
