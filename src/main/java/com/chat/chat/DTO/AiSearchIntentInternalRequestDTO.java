package com.chat.chat.DTO;

public class AiSearchIntentInternalRequestDTO {

    private String requestId;
    private String consulta;
    private String usuarioActualNombre;

    public AiSearchIntentInternalRequestDTO() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getConsulta() { return consulta; }
    public void setConsulta(String consulta) { this.consulta = consulta; }

    public String getUsuarioActualNombre() { return usuarioActualNombre; }
    public void setUsuarioActualNombre(String usuarioActualNombre) { this.usuarioActualNombre = usuarioActualNombre; }
}
