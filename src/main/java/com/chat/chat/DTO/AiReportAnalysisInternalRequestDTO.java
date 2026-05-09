package com.chat.chat.DTO;

import java.util.List;

public class AiReportAnalysisInternalRequestDTO {

    private String requestId;
    private String tipoChat;
    private Long chatId;
    private Long chatGrupalId;
    private Long usuarioDenunciadoId;
    private String nombreUsuarioDenunciado;
    private List<String> motivosDisponibles;
    private List<AiReportAnalysisInternalContextMessageDTO> mensajesContexto;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public Long getUsuarioDenunciadoId() {
        return usuarioDenunciadoId;
    }

    public void setUsuarioDenunciadoId(Long usuarioDenunciadoId) {
        this.usuarioDenunciadoId = usuarioDenunciadoId;
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

    public List<AiReportAnalysisInternalContextMessageDTO> getMensajesContexto() {
        return mensajesContexto;
    }

    public void setMensajesContexto(List<AiReportAnalysisInternalContextMessageDTO> mensajesContexto) {
        this.mensajesContexto = mensajesContexto;
    }
}
