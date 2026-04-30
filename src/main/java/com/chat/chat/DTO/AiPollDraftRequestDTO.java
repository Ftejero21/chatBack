package com.chat.chat.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiPollDraftRequestDTO {

    @NotNull(message = "chatGrupalId es obligatorio")
    private Long chatGrupalId;

    @Valid
    @Size(min = 1, max = 100, message = "Los mensajes son obligatorios y no pueden superar 100 elementos")
    private List<AiPollDraftContextMessageDTO> mensajes;

    @Min(value = 2, message = "maxOpciones debe ser al menos 2")
    @Max(value = 10, message = "maxOpciones no puede superar 10")
    private Integer maxOpciones;

    @Size(max = 20, message = "El estilo no es valido")
    private String estilo;

    public Long getChatGrupalId() {
        return chatGrupalId;
    }

    public void setChatGrupalId(Long chatGrupalId) {
        this.chatGrupalId = chatGrupalId;
    }

    public List<AiPollDraftContextMessageDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<AiPollDraftContextMessageDTO> mensajes) {
        this.mensajes = mensajes;
    }

    public Integer getMaxOpciones() {
        return maxOpciones;
    }

    public void setMaxOpciones(Integer maxOpciones) {
        this.maxOpciones = maxOpciones;
    }

    public String getEstilo() {
        return estilo;
    }

    public void setEstilo(String estilo) {
        this.estilo = estilo;
    }
}
