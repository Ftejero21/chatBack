package com.chat.chat.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiConversationSummaryRequestDTO {

    @Pattern(regexp = "^(INDIVIDUAL|GRUPAL)$", message = "El tipo de chat no es valido")
    private String tipoChat;

    private Long chatId;

    private Long chatGrupalId;

    @Valid
    @NotEmpty(message = "Los mensajes son obligatorios")
    @Size(max = 100, message = "La conversacion supera el numero maximo de mensajes permitido")
    private List<AiConversationMessageDTO> mensajes;

    private Integer maxLineas;

    @Size(max = 20, message = "El estilo no es valido")
    private String estilo;

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

    public List<AiConversationMessageDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<AiConversationMessageDTO> mensajes) {
        this.mensajes = mensajes;
    }

    public Integer getMaxLineas() {
        return maxLineas;
    }

    public void setMaxLineas(Integer maxLineas) {
        this.maxLineas = maxLineas;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }
}
