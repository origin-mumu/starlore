package com.robin.blogback.service;

import com.robin.blogback.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    UserInfo getCurrentUser(HttpServletRequest request);
    AuthResponse updateProfile(HttpServletRequest request, UpdateProfileRequest updateRequest);
}
