package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robin.blogback.entity.LoginLog;
import com.robin.blogback.entity.User;
import com.robin.blogback.exception.BadRequestException;
import com.robin.blogback.exception.ForbiddenException;
import com.robin.blogback.exception.NotFoundException;
import com.robin.blogback.mapper.LoginLogMapper;
import com.robin.blogback.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    /** 检查当前用户是否为管理员 */
    private void requireAdmin(Integer userId) {
        if (userId == null) throw new ForbiddenException("无权限");
        User user = userMapper.selectById(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new ForbiddenException("无权限");
        }
    }

    /** 获取所有用户列表 */
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        requireAdmin(userId);

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername, User::getNickname,
                                User::getEmail, User::getAvatar, User::getBio,
                                User::getLocation, User::getWebsite, User::getGithub,
                                User::getRole, User::getAiDailyLimit, User::getAiTodayCount,
                                User::getCreatedAt, User::getUpdatedAt)
                        .orderByDesc(User::getCreatedAt));

        return ResponseEntity.ok(Map.of("data", users));
    }

    /** 获取单个用户详情 */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        requireAdmin(userId);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        user.setPassword(null);
        return ResponseEntity.ok(Map.of("data", user));
    }

    /** 更新用户信息（管理员） */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(HttpServletRequest request,
                                        @PathVariable Integer id,
                                        @RequestBody Map<String, Object> body) {
        Integer adminId = (Integer) request.getAttribute("userId");
        requireAdmin(adminId);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        if (body.containsKey("nickname")) user.setNickname((String) body.get("nickname"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("bio")) user.setBio((String) body.get("bio"));
        if (body.containsKey("location")) user.setLocation((String) body.get("location"));
        if (body.containsKey("website")) user.setWebsite((String) body.get("website"));
        if (body.containsKey("github")) user.setGithub((String) body.get("github"));
        if (body.containsKey("role")) user.setRole((String) body.get("role"));
        if (body.containsKey("aiDailyLimit")) {
            int limit = ((Number) body.get("aiDailyLimit")).intValue();
            user.setAiDailyLimit(limit);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return ResponseEntity.ok(Map.of("message", "更新成功", "data", user));
    }

    /** 删除用户 */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(HttpServletRequest request, @PathVariable Integer id) {
        Integer adminId = (Integer) request.getAttribute("userId");
        requireAdmin(adminId);

        if (id.equals(adminId)) {
            throw new BadRequestException("不能删除自己");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        userMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
    }

    /** 获取登录日志（分页 + 筛选） */
    @GetMapping("/login-logs")
    public ResponseEntity<?> getLoginLogs(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        Integer userId = (Integer) request.getAttribute("userId");
        requireAdmin(userId);

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>();
        if (StringUtils.hasText(username)) {
            wrapper.eq(LoginLog::getUsername, username);
        }
        if (dateFrom != null) {
            wrapper.ge(LoginLog::getLoginTime, LocalDateTime.parse(dateFrom + "T00:00:00"));
        }
        if (dateTo != null) {
            wrapper.le(LoginLog::getLoginTime, LocalDateTime.parse(dateTo + "T23:59:59"));
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> pageObj = new Page<>(page, limit);
        loginLogMapper.selectPage(pageObj, wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", pageObj.getRecords());
        result.put("total", pageObj.getTotal());
        result.put("pages", pageObj.getPages());
        result.put("current", pageObj.getCurrent());
        return ResponseEntity.ok(result);
    }
}
