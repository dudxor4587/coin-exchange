package com.coinexchange.user.presentation;

import com.coinexchange.user.application.UserService;
import com.coinexchange.user.application.dto.UserLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserLookupResponse> findByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
}
