package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class UserComplaintCreateDTO {
    @NotNull(message = "denunciadoId es obligatorio")
    @Positive(message = "denunciadoId invalido")
    private Long denunciadoId;

    @Positive(message = "chatId invalido")
    private Long chatId;

    @NotBlank(message = "motivo es obligatorio")
    @Size(max = 120, message = "motivo supera el maximo de 120 caracteres")
    private String motivo;

    @NotBlank(message = "detalle es obligatorio")
    @Size(max = 10000, message = "detalle supera el maximo de 10000 caracteres")
    private String detalle;

    @Size(max = 190, message = "denunciadoNombre supera el maximo de 190 caracteres")
    private String denunciadoNombre;

    @Size(max = 190, message = "chatNombreSnapshot supera el maximo de 190 caracteres")
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
