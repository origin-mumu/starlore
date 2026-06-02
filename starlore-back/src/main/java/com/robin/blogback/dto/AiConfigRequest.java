package com.robin.blogback.dto;

import lombok.Data;

@Data
public class AiConfigRequest {
    private String modelKey;
    private String modelName;
    private String apiUrl;
    private String modelId;
    private String apiKey;
    private Boolean enabled;
}
