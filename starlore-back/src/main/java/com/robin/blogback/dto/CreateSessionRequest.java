package com.robin.blogback.dto;

import lombok.Data;

@Data
public class CreateSessionRequest {
    private String title;
    private String characterKey;
    private String modelId;
}
