package com.chat.chat.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class GroupInviteCreateDTO {
    @NotNull(message = "groupId es obligatorio")
    @Positive(message = "groupId invalido")
    private Long groupId;

    @NotNull(message = "inviteeId es obligatorio")
    @Positive(message = "inviteeId invalido")
    private Long inviteeId;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getInviteeId() {
        return inviteeId;
    }

    public void setInviteeId(Long inviteeId) {
        this.inviteeId = inviteeId;
    }
}
