package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.*;
import com.robin.blogback.entity.LoginLog;
import com.robin.blogback.entity.User;
import com.robin.blogback.exception.BadRequestException;
import com.robin.blogback.exception.ConflictException;
import com.robin.blogback.exception.NotFoundException;
import com.robin.blogback.exception.UnauthorizedException;
import com.robin.blogback.mapper.LoginLogMapper;
import com.robin.blogback.mapper.UserMapper;
import com.robin.blogback.service.AuthService;
import com.robin.blogback.service.IpLocationService;
import com.robin.blogback.util.JwtUtil;
import com.robin.blogback.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private IpLocationService ipLocationService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null
                || request.getUsername().isBlank() || request.getPassword().isBlank()) {
            throw new BadRequestException("用户名和密码不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new BadRequestException("密码长度至少6位");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new ConflictException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("user");
        user.setAiDailyLimit(10);
        user.setAiTodayCount(0);
        user.setAiResetDate(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = toUserInfo(user);
        return new AuthResponse("注册成功", new AuthResponse.AuthData(token, userInfo));
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        if (request.getUsername() == null || request.getPassword() == null
                || request.getUsername().isBlank() || request.getPassword().isBlank()) {
            throw new BadRequestException("用户名和密码不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        UserInfo userInfo = toUserInfo(user);

        // 记录登录日志
        try {
            LoginLog log = new LoginLog();
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
            String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
            if (ip != null && ip.equals("0:0:0:0:0:0:0:1")) ip = "127.0.0.1";
            log.setIp(ip);
            log.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(log);

            // 异步查询 IP 归属地并更新
            if (ip != null && !ip.startsWith("127.") && !"0:0:0:0:0:0:0:1".equals(ip)) {
                try {
                    IpLocationService.IpLocation loc = ipLocationService.lookup(ip);
                    if (loc.country() != null && !loc.country().isEmpty()) {
                        log.setCountry(loc.country());
                        log.setProvince(loc.province());
                        log.setCity(loc.city());
                        loginLogMapper.updateById(log);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("登录日志记录失败: " + e.getMessage());
        }

        return new AuthResponse("登录成功", new AuthResponse.AuthData(token, userInfo));
    }

    @Override
    public UserInfo getCurrentUser(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("未提供认证令牌");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return toUserInfo(user);
    }

    @Override
    public AuthResponse updateProfile(HttpServletRequest request, UpdateProfileRequest updateRequest) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("未提供认证令牌");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        if (updateRequest.getNickname() != null) user.setNickname(updateRequest.getNickname());
        if (updateRequest.getAvatar() != null) user.setAvatar(updateRequest.getAvatar());
        if (updateRequest.getBio() != null) user.setBio(updateRequest.getBio());
        if (updateRequest.getEmail() != null) user.setEmail(updateRequest.getEmail());
        if (updateRequest.getLocation() != null) user.setLocation(updateRequest.getLocation());
        if (updateRequest.getWebsite() != null) user.setWebsite(updateRequest.getWebsite());
        if (updateRequest.getGithub() != null) user.setGithub(updateRequest.getGithub());

        // 修改用户名
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(user.getUsername())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, updateRequest.getUsername()));
            if (count > 0) {
                throw new ConflictException("用户名已被使用");
            }
            user.setUsername(updateRequest.getUsername());
        }

        // 修改密码
        if (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().isEmpty()) {
            if (updateRequest.getOldPassword() == null || updateRequest.getOldPassword().isEmpty()) {
                throw new BadRequestException("请输入原密码");
            }
            if (!passwordUtil.matches(updateRequest.getOldPassword(), user.getPassword())) {
                throw new BadRequestException("原密码错误");
            }
            user.setPassword(passwordUtil.encode(updateRequest.getNewPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserInfo userInfo = toUserInfo(user);
        return new AuthResponse("更新成功", new AuthResponse.AuthData(null, userInfo));
    }

    private UserInfo toUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setEmail(user.getEmail());
        info.setAvatar(user.getAvatar());
        info.setBio(user.getBio());
        info.setLocation(user.getLocation());
        info.setWebsite(user.getWebsite());
        info.setGithub(user.getGithub());
        info.setRole(user.getRole());
        info.setAiDailyLimit(user.getAiDailyLimit());
        info.setAiTodayCount(user.getAiTodayCount());
        info.setCreatedAt(user.getCreatedAt());
        info.setUpdatedAt(user.getUpdatedAt());
        return info;
    }
}
