package com.robin.blogback.dto;

import lombok.Data;

@Data
public class UpdateSessionRequest {
    private String title;
    private String characterKey;
    private String modelId;
}
