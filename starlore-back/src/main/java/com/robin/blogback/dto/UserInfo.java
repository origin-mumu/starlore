package com.robin.blogback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfo {
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    private String location;
    private String website;
    private String github;
    private String role;
    private Integer aiDailyLimit;
    private Integer aiTodayCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
}
