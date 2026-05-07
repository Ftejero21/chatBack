package com.chat.chat.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiReportAnalysisRequestDTO {

    @NotNull(message = "usuarioDenunciadoId es obligatorio")
    private Long usuarioDenunciadoId;

    @Size(max = 20, message = "El tipo de chat no es valido")
    private String tipoChat;

    private Long chatId;

    private Long chatGrupalId;

    @Size(max = 120, message = "El nombre del usuario denunciado supera la longitud maxima permitida")
    private String nombreUsuarioDenunciado;

    @Valid
    @Size(max = 50, message = "Los motivos disponibles no pueden superar 50 elementos")
    private List<@Size(max = 100, message = "Un motivo disponible supera la longitud maxima permitida") String> motivosDisponibles;

    @Valid
    @Size(max = 200, message = "Los mensajes no pueden superar 200 elementos")
    private List<AiReportContextMessageDTO> mensajes;

    @Min(value = 1, message = "maxMensajes debe ser al menos 1")
    @Max(value = 200, message = "maxMensajes no puede superar 200")
    private Integer maxMensajes;

    public Long getUsuarioDenunciadoId() {
        return usuarioDenunciadoId;
    }

    public void setUsuarioDenunciadoId(Long usuarioDenunciadoId) {
        this.usuarioDenunciadoId = usuarioDenunciadoId;
    }

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatGrupalId() {
        return chatGrupalId;
    }

    public void setChatGrupalId(Long chatGrupalId) {
        this.chatGrupalId = chatGrupalId;
    }

    public String getNombreUsuarioDenunciado() {
        return nombreUsuarioDenunciado;
    }

    public void setNombreUsuarioDenunciado(String nombreUsuarioDenunciado) {
        this.nombreUsuarioDenunciado = nombreUsuarioDenunciado;
    }

    public List<String> getMotivosDisponibles() {
        return motivosDisponibles;
    }

    public void setMotivosDisponibles(List<String> motivosDisponibles) {
        this.motivosDisponibles = motivosDisponibles;
    }

    public List<AiReportContextMessageDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<AiReportContextMessageDTO> mensajes) {
        this.mensajes = mensajes;
    }

    public Integer getMaxMensajes() {
        return maxMensajes;
    }

    public void setMaxMensajes(Integer maxMensajes) {
        this.maxMensajes = maxMensajes;
    }
}
