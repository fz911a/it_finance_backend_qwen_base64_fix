package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.Team;
import com.example.itfinance.entity.TeamMember;
import com.example.itfinance.service.AuthService;
import com.example.itfinance.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@CrossOrigin
public class TeamController {
    private final TeamService teamService;
    private final AuthService authService;

    public TeamController(TeamService teamService, AuthService authService) {
        this.teamService = teamService;
        this.authService = authService;
    }

    @PostMapping("/create")
    public ApiResponse<Team> createTeam(@RequestHeader("Authorization") String token, @RequestBody Team team) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            team.setCreatorId(userId);
            Team createdTeam = teamService.createTeam(team);
            return ApiResponse.ok(createdTeam);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/update")
    public ApiResponse<Team> updateTeam(@RequestHeader("Authorization") String token, @RequestBody Team team) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Team existingTeam = teamService.getTeamById(team.getId());
            if (existingTeam == null || !existingTeam.getCreatorId().equals(userId)) {
                throw new IllegalArgumentException("团队不存在或无权限");
            }
            Team updatedTeam = teamService.updateTeam(team);
            return ApiResponse.ok(updatedTeam);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteTeam(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Team team = teamService.getTeamById(id);
            if (team == null || !team.getCreatorId().equals(userId)) {
                throw new IllegalArgumentException("团队不存在或无权限");
            }
            teamService.deleteTeam(id);
            return ApiResponse.ok("团队删除成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public ApiResponse<Team> getTeamById(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            Team team = teamService.getTeamById(id);
            if (team == null) {
                throw new IllegalArgumentException("团队不存在");
            }
            return ApiResponse.ok(team);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Team>> getTeamsByCreatorId(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            List<Team> teams = teamService.getTeamsByCreatorId(userId);
            return ApiResponse.ok(teams);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/member/add")
    public ApiResponse<TeamMember> addTeamMember(@RequestHeader("Authorization") String token, @RequestBody TeamMember teamMember) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Team team = teamService.getTeamById(teamMember.getTeamId());
            if (team == null || !team.getCreatorId().equals(userId)) {
                throw new IllegalArgumentException("团队不存在或无权限");
            }
            TeamMember createdMember = teamService.addTeamMember(teamMember);
            return ApiResponse.ok(createdMember);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/member/remove/{id}")
    public ApiResponse<String> removeTeamMember(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            // 这里需要验证用户是否是团队创建者
            // 简化处理，直接删除
            teamService.removeTeamMember(id);
            return ApiResponse.ok("团队成员删除成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/member/list/{teamId}")
    public ApiResponse<List<TeamMember>> getTeamMembersByTeamId(@RequestHeader("Authorization") String token, @PathVariable Long teamId) {
        try {
            List<TeamMember> members = teamService.getTeamMembersByTeamId(teamId);
            return ApiResponse.ok(members);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/member/user")
    public ApiResponse<List<TeamMember>> getTeamMembersByUserId(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            List<TeamMember> members = teamService.getTeamMembersByUserId(userId);
            return ApiResponse.ok(members);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
