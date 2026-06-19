package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private String bio;
    private String location;
    private String website;
    private String github;
    private String role;
    @TableField("ai_daily_limit")
    private Integer aiDailyLimit;
    @TableField("ai_today_count")
    private Integer aiTodayCount;
    @TableField("ai_reset_date")
    private LocalDate aiResetDate;
    @TableField("register_ip")
    private String registerIp;
    @TableField("register_country")
    private String registerCountry;
    @TableField("register_province")
    private String registerProvince;
    @TableField("register_city")
    private String registerCity;
    @TableField("createdAt")
    private LocalDateTime createdAt;
    @TableField("updatedAt")
    private LocalDateTime updatedAt;
}
