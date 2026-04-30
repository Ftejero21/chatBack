package com.chat.chat.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiQuickReplyRequestDTO {

    @NotBlank(message = "El mensaje recibido es obligatorio")
    @Size(max = 2000, message = "El mensaje recibido supera la longitud maxima permitida")
    private String mensajeRecibido;

    @NotBlank(message = "El tipo de chat es obligatorio")
    @Pattern(regexp = "^(INDIVIDUAL|GRUPAL)$", message = "El tipo de chat no es valido")
    private String tipoChat;

    @Valid
    private List<AiChatContextMessageDTO> contexto;

    private Long messageId;

    private Long chatId;

    private Long chatGrupalId;

    public String getMensajeRecibido() {
        return mensajeRecibido;
    }

    public void setMensajeRecibido(String mensajeRecibido) {
        this.mensajeRecibido = mensajeRecibido;
    }

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public List<AiChatContextMessageDTO> getContexto() {
        return contexto;
    }

    public void setContexto(List<AiChatContextMessageDTO> contexto) {
        this.contexto = contexto;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
}
