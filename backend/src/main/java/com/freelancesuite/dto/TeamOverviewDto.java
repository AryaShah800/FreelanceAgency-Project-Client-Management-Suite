package com.freelancesuite.dto;

import java.util.List;

public class TeamOverviewDto {
    private List<TeamMemberDto> members;
    private List<PendingInvitationDto> invitations;
    public TeamOverviewDto() {}
    public TeamOverviewDto(List<TeamMemberDto> members, List<PendingInvitationDto> invitations) {
        this.members = members; this.invitations = invitations;
    }
    public List<TeamMemberDto> getMembers() { return members; }
    public List<PendingInvitationDto> getInvitations() { return invitations; }
}
