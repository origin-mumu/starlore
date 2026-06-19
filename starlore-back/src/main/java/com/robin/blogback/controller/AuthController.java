package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(HttpServletRequest httpRequest, @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest httpRequest, @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        UserInfo user = authService.getCurrentUser(request);
        return ResponseEntity.ok(Map.of("data", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateProfileRequest updateRequest) {
        AuthResponse response = authService.updateProfile(request, updateRequest);
        return ResponseEntity.ok(response);
    }
}
