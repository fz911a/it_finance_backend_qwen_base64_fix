package com.example.itfinance.service;

import com.example.itfinance.entity.Team;
import com.example.itfinance.entity.TeamMember;
import java.util.List;

public interface TeamService {
    Team createTeam(Team team);
    Team updateTeam(Team team);
    void deleteTeam(Long id);
    Team getTeamById(Long id);
    List<Team> getTeamsByCreatorId(Long creatorId);
    TeamMember addTeamMember(TeamMember teamMember);
    void removeTeamMember(Long id);
    List<TeamMember> getTeamMembersByTeamId(Long teamId);
    List<TeamMember> getTeamMembersByUserId(Long userId);
}