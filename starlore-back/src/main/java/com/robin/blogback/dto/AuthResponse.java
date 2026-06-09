package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String message;
    private AuthData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthData {
        private String token;
        private UserInfo user;
    }
}
