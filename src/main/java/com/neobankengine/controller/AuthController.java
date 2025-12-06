package com.neobankengine.controller;

import com.neobankengine.dto.LoginRequest;
import com.neobankengine.dto.LoginResponse;
import com.neobankengine.dto.UserRegisterRequest;
import com.neobankengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = userService.loginAndGetToken(request.getEmail(), request.getPassword());
        if (token == null) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }

}

