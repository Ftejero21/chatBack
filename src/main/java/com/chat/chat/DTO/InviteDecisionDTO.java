package com.chat.chat.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class InviteDecisionDTO {
    @NotNull(message = "userId es obligatorio")
    @Positive(message = "userId invalido")
    private Long userId; // quien acepta/declina
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }


}
