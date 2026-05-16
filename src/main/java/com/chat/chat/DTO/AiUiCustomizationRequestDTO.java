package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiUiCustomizationRequestDTO {

    @NotBlank(message = "consulta es obligatoria")
    @Size(max = 400, message = "consulta supera el maximo")
    private String consulta;
    private UiCustomizationContextDTO uiContext;

    public AiUiCustomizationRequestDTO() {}

    public String getConsulta() { return consulta; }
    public void setConsulta(String consulta) { this.consulta = consulta; }

    public UiCustomizationContextDTO getUiContext() { return uiContext; }
    public void setUiContext(UiCustomizationContextDTO uiContext) { this.uiContext = uiContext; }
}
