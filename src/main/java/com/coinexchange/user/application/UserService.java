package com.coinexchange.user.application;

import com.coinexchange.common.auth.JwtTokenProvider;
import com.coinexchange.user.domain.User;
import com.coinexchange.user.domain.repository.UserRepository;
import com.coinexchange.user.exception.UserException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.coinexchange.user.exception.UserExceptionType.EMAIL_ALREADY_EXISTS;
import static com.coinexchange.user.exception.UserExceptionType.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        user.isMatchPassword(password, passwordEncoder);

        return jwtTokenProvider.createToken(user.getId(), user.getRole().name());
    }

    public void signUp(String email, String password, String name, String phoneNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .phone(phoneNumber)
                .role(User.Role.USER)
                .build();
        userRepository.save(user);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        long expirationTime = jwtTokenProvider.getExpirationTime(accessToken);

        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) expirationTime);

        response.addCookie(cookie);
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

}
