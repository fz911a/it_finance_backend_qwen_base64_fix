package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Project;
import com.example.itfinance.service.ProjectService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    private final JdbcTemplate jdbcTemplate;

    public ProjectServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Project mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Project(
                rs.getLong("id"),
                rs.getString("project_name"),
                rs.getString("project_code"),
                rs.getString("manager_name"),
                rs.getBigDecimal("budget_amount"));
    }

    @Override
    public List<Project> list() {
        return jdbcTemplate.query(
                "SELECT p.id, p.project_name, p.project_code, u.real_name manager_name, p.budget_amount FROM project p LEFT JOIN sys_user u ON p.manager_id = u.id ORDER BY p.id",
                this::mapRow);
    }

    @Override
    public Project create(Project project) {
        jdbcTemplate.update(
                "INSERT INTO project(project_name, project_code, customer_name, budget_amount, status) VALUES (?,?,?,?,?)",
                project.getProjectName(), project.getProjectCode(), project.getManagerName(),
                project.getBudgetAmount(), "进行中");
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getById(id);
    }

    @Override
    public Project update(Project project) {
        jdbcTemplate.update(
                "UPDATE project SET project_name=?, project_code=?, budget_amount=? WHERE id=?",
                project.getProjectName(), project.getProjectCode(), project.getBudgetAmount(), project.getId());
        return getById(project.getId());
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM project WHERE id=?", id);
    }

    private Project getById(Long id) {
        List<Project> list = jdbcTemplate.query(
                "SELECT p.id, p.project_name, p.project_code, u.real_name manager_name, p.budget_amount FROM project p LEFT JOIN sys_user u ON p.manager_id = u.id WHERE p.id=?",
                this::mapRow, id);
        return list.isEmpty() ? null : list.get(0);
    }
}
