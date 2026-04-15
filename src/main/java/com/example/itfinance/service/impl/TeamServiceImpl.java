package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Team;
import com.example.itfinance.entity.TeamMember;
import com.example.itfinance.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamServiceImpl implements TeamService {
    private final Map<Long, Team> teamMap = new HashMap<>();
    private final Map<Long, TeamMember> teamMemberMap = new HashMap<>();
    private Long nextTeamId = 1L;
    private Long nextTeamMemberId = 1L;

    @Override
    public Team createTeam(Team team) {
        team.setId(nextTeamId++);
        teamMap.put(team.getId(), team);
        return team;
    }

    @Override
    public Team updateTeam(Team team) {
        if (teamMap.containsKey(team.getId())) {
            teamMap.put(team.getId(), team);
            return team;
        }
        return null;
    }

    @Override
    public void deleteTeam(Long id) {
        teamMap.remove(id);
        // 同时删除该团队的所有成员
        List<Long> memberIdsToRemove = new ArrayList<>();
        for (TeamMember member : teamMemberMap.values()) {
            if (member.getTeamId().equals(id)) {
                memberIdsToRemove.add(member.getId());
            }
        }
        for (Long memberId : memberIdsToRemove) {
            teamMemberMap.remove(memberId);
        }
    }

    @Override
    public Team getTeamById(Long id) {
        return teamMap.get(id);
    }

    @Override
    public List<Team> getTeamsByCreatorId(Long creatorId) {
        List<Team> teams = new ArrayList<>();
        for (Team team : teamMap.values()) {
            if (team.getCreatorId().equals(creatorId)) {
                teams.add(team);
            }
        }
        return teams;
    }

    @Override
    public TeamMember addTeamMember(TeamMember teamMember) {
        teamMember.setId(nextTeamMemberId++);
        teamMemberMap.put(teamMember.getId(), teamMember);
        return teamMember;
    }

    @Override
    public void removeTeamMember(Long id) {
        teamMemberMap.remove(id);
    }

    @Override
    public List<TeamMember> getTeamMembersByTeamId(Long teamId) {
        List<TeamMember> members = new ArrayList<>();
        for (TeamMember member : teamMemberMap.values()) {
            if (member.getTeamId().equals(teamId)) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public List<TeamMember> getTeamMembersByUserId(Long userId) {
        List<TeamMember> members = new ArrayList<>();
        for (TeamMember member : teamMemberMap.values()) {
            if (member.getUserId().equals(userId)) {
                members.add(member);
            }
        }
        return members;
    }
}