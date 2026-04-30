package com.chat.chat.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiEncryptedConversationSummaryRequestDTO {

    @NotBlank(message = "El tipo de chat es obligatorio")
    @Size(max = 20, message = "El tipo de chat no es valido")
    private String tipoChat;

    private Long chatId;

    private Long chatGrupalId;

    @Valid
    @Size(min = 1, max = 200, message = "Los mensajes son obligatorios y no pueden superar 200 elementos")
    private List<AiEncryptedContextMessageDTO> mensajes;

    @Min(value = 1, message = "maxLineas debe ser al menos 1")
    @Max(value = 12, message = "maxLineas no puede superar 12")
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

    public List<AiEncryptedContextMessageDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<AiEncryptedContextMessageDTO> mensajes) {
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
