package com.chat.chat.DTO;

import java.util.List;

public class AiSemanticRerankInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private Long selectedId;
    private java.util.List<Long> selectedIds;
    private Double confidence;
    private String motivoCoincidencia;
    private List<Long> idsOrdenados;
    private Boolean needsClarification;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Long getSelectedId() { return selectedId; }
    public void setSelectedId(Long selectedId) { this.selectedId = selectedId; }

    public java.util.List<Long> getSelectedIds() { return selectedIds; }
    public void setSelectedIds(java.util.List<Long> selectedIds) { this.selectedIds = selectedIds; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getMotivoCoincidencia() { return motivoCoincidencia; }
    public void setMotivoCoincidencia(String motivoCoincidencia) { this.motivoCoincidencia = motivoCoincidencia; }

    public List<Long> getIdsOrdenados() { return idsOrdenados; }
    public void setIdsOrdenados(List<Long> idsOrdenados) { this.idsOrdenados = idsOrdenados; }

    public Boolean getNeedsClarification() { return needsClarification; }
    public void setNeedsClarification(Boolean needsClarification) { this.needsClarification = needsClarification; }
}
