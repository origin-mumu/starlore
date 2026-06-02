package com.robin.blogback.service;

import com.robin.blogback.dto.ProjectInfo;
import com.robin.blogback.dto.ProjectRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ProjectService {
    List<ProjectInfo> listProjects(Integer userId);

    ProjectInfo createProject(HttpServletRequest request, ProjectRequest req);

    ProjectInfo updateProject(HttpServletRequest request, Integer projectId, ProjectRequest req);

    void deleteProject(HttpServletRequest request, Integer projectId);
}
