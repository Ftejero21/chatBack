package com.chat.chat.DTO;

import java.time.Instant;

public class ChatCloseStateDTO {

    private boolean ok;
    private Long chatId;
    private boolean closed;
    private boolean cerrado;
    private String reason;
    private String motivo;
    private Instant closedAt;
    private Long closedByAdminId;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isCerrado() {
        return cerrado;
    }

    public void setCerrado(boolean cerrado) {
        this.cerrado = cerrado;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Long getClosedByAdminId() {
        return closedByAdminId;
    }

    public void setClosedByAdminId(Long closedByAdminId) {
        this.closedByAdminId = closedByAdminId;
    }
}
