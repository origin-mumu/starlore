package com.robin.blogback.service;

import com.robin.blogback.dto.AiConfigRequest;
import com.robin.blogback.entity.AiConfig;

import java.util.List;

public interface AiConfigService {
    List<AiConfig> getAllConfigs();
    AiConfig getConfigById(Integer id);
    AiConfig createConfig(AiConfigRequest request);
    AiConfig updateConfig(Integer id, AiConfigRequest request);
    void deleteConfig(Integer id);
    AiConfig getConfigByKey(String modelKey);
}
