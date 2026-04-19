package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.Project;
import com.example.itfinance.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@CrossOrigin
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Project>> list() {
        return ApiResponse.ok(projectService.list());
    }

    @PostMapping("/add")
    public ApiResponse<Project> add(@RequestBody Project project) {
        try {
            return ApiResponse.ok("新增成功", projectService.create(project));
        } catch (Exception e) {
            return ApiResponse.fail("项目新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Project> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        try {
            return ApiResponse.ok("更新成功", projectService.update(project));
        } catch (Exception e) {
            return ApiResponse.fail("项目更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        projectService.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
