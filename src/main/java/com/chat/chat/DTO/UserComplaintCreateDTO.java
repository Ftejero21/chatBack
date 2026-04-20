package com.chat.chat.DTO;

public class UserComplaintCreateDTO {
    private Long denunciadoId;
    private Long chatId;
    private String motivo;
    private String detalle;
    private String denunciadoNombre;
    private String chatNombreSnapshot;

    public Long getDenunciadoId() {
        return denunciadoId;
    }

    public void setDenunciadoId(Long denunciadoId) {
        this.denunciadoId = denunciadoId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getDenunciadoNombre() {
        return denunciadoNombre;
    }

    public void setDenunciadoNombre(String denunciadoNombre) {
        this.denunciadoNombre = denunciadoNombre;
    }

    public String getChatNombreSnapshot() {
        return chatNombreSnapshot;
    }

    public void setChatNombreSnapshot(String chatNombreSnapshot) {
        this.chatNombreSnapshot = chatNombreSnapshot;
    }
}
