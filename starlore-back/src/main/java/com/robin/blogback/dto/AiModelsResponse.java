package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiModelsResponse {
    private boolean success;
    private List<ModelInfo> models;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModelInfo {
        private String id;
        private String name;
        private boolean configured;
    }
}
