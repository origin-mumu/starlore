package com.robin.blogback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_logs")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Integer userId;
    private String username;
    private String ip;
    private String country;
    private String province;
    private String city;
    @TableField("user_agent")
    private String userAgent;
    @TableField("login_time")
    private LocalDateTime loginTime;
}
