package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.entity.Resume;
import com.robin.blogback.mapper.ResumeMapper;
import com.robin.blogback.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private ResumeMapper resumeMapper;

    @Override
    public List<Resume> listByUser(Integer userId) {
        return resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getStatus, "active")
                        .orderByDesc(Resume::getUpdatedAt));
    }

    @Override
    public Resume getById(Integer id, Integer userId) {
        return resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, id)
                        .eq(Resume::getUserId, userId));
    }

    @Override
    public Resume create(Resume resume, Integer userId) {
        resume.setUserId(userId);
        resume.setStatus("active");
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        return resume;
    }

    @Override
    public Resume update(Resume resume, Integer userId) {
        Resume existing = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resume.getId())
                        .eq(Resume::getUserId, userId));
        if (existing == null) throw new RuntimeException("简历不存在");
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return resumeMapper.selectById(resume.getId());
    }

    @Override
    public void delete(Integer id, Integer userId) {
        Resume existing = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, id)
                        .eq(Resume::getUserId, userId));
        if (existing == null) throw new RuntimeException("简历不存在");
        existing.setStatus("deleted");
        existing.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(existing);
    }
}
