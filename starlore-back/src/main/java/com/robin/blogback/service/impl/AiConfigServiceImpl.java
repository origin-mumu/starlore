package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.AiConfigRequest;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.mapper.AiConfigMapper;
import com.robin.blogback.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConfigServiceImpl implements AiConfigService {

    @Autowired
    private AiConfigMapper aiConfigMapper;

    @Override
    public List<AiConfig> getAllConfigs() {
        return aiConfigMapper.selectList(
                new LambdaQueryWrapper<AiConfig>().orderByAsc(AiConfig::getModelKey));
    }

    @Override
    public AiConfig getConfigById(Integer id) {
        AiConfig config = aiConfigMapper.selectById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        return config;
    }

    @Override
    public AiConfig getConfigByKey(String modelKey) {
        return aiConfigMapper.selectOne(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getModelKey, modelKey));
    }

    @Override
    public AiConfig createConfig(AiConfigRequest request) {
        if (request.getModelKey() == null || request.getModelName() == null
                || request.getApiUrl() == null || request.getModelId() == null) {
            throw new IllegalStateException("modelKey、modelName、apiUrl、modelId 为必填项");
        }
        AiConfig existing = getConfigByKey(request.getModelKey());
        if (existing != null) {
            throw new IllegalStateException("模型标识 " + request.getModelKey() + " 已存在");
        }
        AiConfig config = new AiConfig();
        config.setModelKey(request.getModelKey());
        config.setModelName(request.getModelName());
        config.setApiUrl(request.getApiUrl());
        config.setModelId(request.getModelId());
        config.setApiKey(request.getApiKey());
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        aiConfigMapper.insert(config);
        return config;
    }

    @Override
    public AiConfig updateConfig(Integer id, AiConfigRequest request) {
        AiConfig config = getConfigById(id);
        if (request.getModelKey() != null && !request.getModelKey().equals(config.getModelKey())) {
            AiConfig existing = getConfigByKey(request.getModelKey());
            if (existing != null) {
                throw new IllegalStateException("模型标识 " + request.getModelKey() + " 已存在");
            }
            config.setModelKey(request.getModelKey());
        }
        if (request.getModelName() != null) config.setModelName(request.getModelName());
        if (request.getApiUrl() != null) config.setApiUrl(request.getApiUrl());
        if (request.getModelId() != null) config.setModelId(request.getModelId());
        if (request.getApiKey() != null) config.setApiKey(request.getApiKey());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());
        config.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.updateById(config);
        return config;
    }

    @Override
    public void deleteConfig(Integer id) {
        AiConfig config = getConfigById(id);
        aiConfigMapper.deleteById(id);
    }
}
