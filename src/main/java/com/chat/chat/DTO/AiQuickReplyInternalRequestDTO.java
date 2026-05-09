package com.chat.chat.DTO;

import java.util.List;

public class AiQuickReplyInternalRequestDTO {

    private String requestId;
    private String tipoChat;
    private Long chatId;
    private Long chatGrupalId;
    private String mensajeRecibido;
    private List<AiQuickReplyInternalContextDTO> contexto;

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

    public String getMensajeRecibido() {
        return mensajeRecibido;
    }

    public void setMensajeRecibido(String mensajeRecibido) {
        this.mensajeRecibido = mensajeRecibido;
    }

    public List<AiQuickReplyInternalContextDTO> getContexto() {
        return contexto;
    }

    public void setContexto(List<AiQuickReplyInternalContextDTO> contexto) {
        this.contexto = contexto;
    }
}
