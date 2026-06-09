package com.robin.blogback.service;

import com.robin.blogback.entity.Resume;
import java.util.List;

public interface ResumeService {
    List<Resume> listByUser(Integer userId);
    Resume getById(Integer id, Integer userId);
    Resume create(Resume resume, Integer userId);
    Resume update(Resume resume, Integer userId);
    void delete(Integer id, Integer userId);
}
