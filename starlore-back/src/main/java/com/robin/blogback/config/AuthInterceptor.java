package com.robin.blogback.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(header.substring(7));
                Integer userId = claims.get("id", Integer.class);
                request.setAttribute("userId", userId);
                request.setAttribute("username", claims.getSubject());
                  return true;
            } catch (Exception e) {
                // token 无效或过期
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已过期，请重新登录");
                return false;
            }
        }

        // 未携带 token
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录，请先登录");
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("message", message)));
    }
}
