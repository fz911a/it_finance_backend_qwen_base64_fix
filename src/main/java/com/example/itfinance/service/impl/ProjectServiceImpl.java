package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Project;
import com.example.itfinance.service.ProjectService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    @Override
    public List<Project> list() {
        return List.of(
                new Project(1L, "智慧运维平台", "IT2026001", "张岂鸣", new BigDecimal("300000")),
                new Project(2L, "企业数据中台", "IT2026002", "王翰洋", new BigDecimal("500000"))
        );
    }
}
