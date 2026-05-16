package com.chat.chat.DTO;

public class AiUiCustomizationIntentInternalRequestDTO {

    private String requestId;
    private String consulta;
    private String usuarioActualNombre;
    private UiCustomizationContextDTO uiContext;

    public AiUiCustomizationIntentInternalRequestDTO() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getConsulta() { return consulta; }
    public void setConsulta(String consulta) { this.consulta = consulta; }

    public String getUsuarioActualNombre() { return usuarioActualNombre; }
    public void setUsuarioActualNombre(String usuarioActualNombre) { this.usuarioActualNombre = usuarioActualNombre; }

    public UiCustomizationContextDTO getUiContext() { return uiContext; }
    public void setUiContext(UiCustomizationContextDTO uiContext) { this.uiContext = uiContext; }
}
