package com.robin.blogback.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robin.blogback.entity.User;
import com.robin.blogback.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AiQuotaService {

    @Autowired
    private UserMapper userMapper;

    /** 获取用户今日 AI 剩余次数（返回 -1 表示不限） */
    public int getRemaining(Integer userId) {
        if (userId == null) return 0;
        User user = userMapper.selectById(userId);
        if (user == null) return 0;

        // 管理员不限
        if ("admin".equals(user.getRole())) return -1;

        // 检查是否需要重置今日计数
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getAiResetDate())) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAiTodayCount, 0)
                    .set(User::getAiResetDate, today));
            return user.getAiDailyLimit() != null ? user.getAiDailyLimit() : 10;
        }

        int limit = user.getAiDailyLimit() != null ? user.getAiDailyLimit() : 10;
        int used = user.getAiTodayCount() != null ? user.getAiTodayCount() : 0;
        return Math.max(0, limit - used);
    }

    /** 尝试消耗一次配额，返回 true=成功，false=配额不足 */
    public boolean tryConsume(Integer userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        if (user == null) return false;

        // 管理员不限
        if ("admin".equals(user.getRole())) return true;

        LocalDate today = LocalDate.now();
        int limit = user.getAiDailyLimit() != null ? user.getAiDailyLimit() : 10;

        // 跨天重置
        if (!today.equals(user.getAiResetDate())) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAiTodayCount, 1)
                    .set(User::getAiResetDate, today));
            return true;
        }

        int used = user.getAiTodayCount() != null ? user.getAiTodayCount() : 0;
        if (used >= limit) return false;

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("ai_today_count = ai_today_count + 1"));
        return true;
    }

    /** 获取用户配额详情 */
    public QuotaInfo getQuotaInfo(Integer userId) {
        if (userId == null) return new QuotaInfo(0, 0, 0, false);
        User user = userMapper.selectById(userId);
        if (user == null) return new QuotaInfo(0, 0, 0, false);

        LocalDate today = LocalDate.now();
        int limit = user.getAiDailyLimit() != null ? user.getAiDailyLimit() : 10;
        int used = user.getAiTodayCount() != null ? user.getAiTodayCount() : 0;

        // 跨天重置
        if (!today.equals(user.getAiResetDate())) {
            used = 0;
        }

        boolean isAdmin = "admin".equals(user.getRole());
        return new QuotaInfo(isAdmin ? -1 : limit, used,
                isAdmin ? -1 : Math.max(0, limit - used), isAdmin);
    }

    public record QuotaInfo(int dailyLimit, int used, int remaining, boolean isAdmin) {}
}
