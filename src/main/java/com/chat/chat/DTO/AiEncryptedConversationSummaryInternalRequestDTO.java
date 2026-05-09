package com.chat.chat.DTO;

import java.util.List;

public class AiEncryptedConversationSummaryInternalRequestDTO {

    private String requestId;
    private Long chatId;
    private Long chatGrupalId;
    private String tipoChat;
    private String estilo;
    private Integer maxLineas;
    private List<AiEncryptedConversationSummaryInternalMessageDTO> mensajes;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }

    public Integer getMaxLineas() {
        return maxLineas;
    }

    public void setMaxLineas(Integer maxLineas) {
        this.maxLineas = maxLineas;
    }

    public List<AiEncryptedConversationSummaryInternalMessageDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<AiEncryptedConversationSummaryInternalMessageDTO> mensajes) {
        this.mensajes = mensajes;
    }
}
