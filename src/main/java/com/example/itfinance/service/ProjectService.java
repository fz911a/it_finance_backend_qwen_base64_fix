package com.example.itfinance.service;

import com.example.itfinance.entity.Project;

import java.util.List;

public interface ProjectService {
    List<Project> list();

    Project create(Project project);

    Project update(Project project);

    void deleteById(Long id);
}
