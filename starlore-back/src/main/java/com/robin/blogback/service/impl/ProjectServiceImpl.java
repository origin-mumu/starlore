package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.ProjectInfo;
import com.robin.blogback.dto.ProjectRequest;
import com.robin.blogback.entity.Project;
import com.robin.blogback.mapper.ProjectMapper;
import com.robin.blogback.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public List<ProjectInfo> listProjects(Integer userId) {
        List<Project> projects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByAsc(Project::getSortOrder)
                        .orderByDesc(Project::getCreatedAt));
        return projects.stream().map(this::toInfo).collect(Collectors.toList());
    }

    @Override
    public ProjectInfo createProject(HttpServletRequest request, ProjectRequest req) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) throw new SecurityException("未提供认证令牌");

        Project project = new Project();
        project.setUserId(userId);
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setUrl(req.getUrl());
        project.setImage(req.getImage());
        project.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insert(project);
        return toInfo(project);
    }

    @Override
    public ProjectInfo updateProject(HttpServletRequest request, Integer projectId, ProjectRequest req) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) throw new SecurityException("未提供认证令牌");

        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }

        if (req.getName() != null) project.setName(req.getName());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        if (req.getUrl() != null) project.setUrl(req.getUrl());
        if (req.getImage() != null) project.setImage(req.getImage());
        if (req.getSortOrder() != null) project.setSortOrder(req.getSortOrder());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return toInfo(project);
    }

    @Override
    public void deleteProject(HttpServletRequest request, Integer projectId) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) throw new SecurityException("未提供认证令牌");

        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        projectMapper.deleteById(projectId);
    }

    private ProjectInfo toInfo(Project project) {
        ProjectInfo info = new ProjectInfo();
        info.setId(project.getId());
        info.setName(project.getName());
        info.setDescription(project.getDescription());
        info.setUrl(project.getUrl());
        info.setImage(project.getImage());
        info.setSortOrder(project.getSortOrder());
        info.setCreatedAt(project.getCreatedAt());
        info.setUpdatedAt(project.getUpdatedAt());
        return info;
    }
}
