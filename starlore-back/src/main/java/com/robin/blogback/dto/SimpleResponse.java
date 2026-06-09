package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleResponse {
    private boolean success;
    private String message;
    private Object data;

    public static SimpleResponse ok(String message) {
        return new SimpleResponse(true, message, null);
    }

    public static SimpleResponse ok(String message, Object data) {
        return new SimpleResponse(true, message, data);
    }

    public static SimpleResponse fail(String message) {
        return new SimpleResponse(false, message, null);
    }

    public static SimpleResponse fail(String message, Object data) {
        return new SimpleResponse(false, message, data);
    }
}
