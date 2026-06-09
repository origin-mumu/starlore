package com.robin.blogback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 20, message = "昵称长度2-20个字符")
    private String nickname;

    private String avatar;

    @Size(max = 500, message = "简介不能超过500个字符")
    private String bio;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String location;
    private String website;
    private String github;

    @Size(min = 2, max = 20, message = "用户名长度2-20个字符")
    private String username;

    private String oldPassword;

    @Size(min = 6, max = 50, message = "新密码长度至少6位")
    private String newPassword;
}
