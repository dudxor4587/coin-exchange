package com.coinexchange.user.presentation;

import com.coinexchange.user.application.UserService;
import com.coinexchange.user.presentation.dto.LoginRequest;
import com.coinexchange.user.presentation.dto.SignUpRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/user/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest,
                                                     HttpServletResponse response) {
        String accessToken = userService.login(
                loginRequest.email(),
                loginRequest.password()
        );

        userService.setAccessTokenCookie(response, accessToken);

        return ResponseEntity.ok("로그인에 성공하였습니다.");
    }

    @PostMapping("/user/sign-up")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest signUpRequest) {
        userService.signUp(
                signUpRequest.email(),
                signUpRequest.password(),
                signUpRequest.name(),
                signUpRequest.phoneNumber()
        );
        return ResponseEntity.ok("회원가입에 성공하였습니다.");
    }

    @PostMapping("/user/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        userService.logout(response);

        return ResponseEntity.ok("로그아웃에 성공하였습니다.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<String> getUserInfo() {
        return ResponseEntity.ok("User Info");
    }
}
