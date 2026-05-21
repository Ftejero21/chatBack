package com.chat.chat.DTO;

import java.util.List;

public class AiUiCustomizationResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String target;
    private String action;
    private String area;
    private String property;
    private String value;
    private String valuePreset;
    private String label;
    private Double confidence;
    private ColorIntentDTO colorIntent;
    private List<UiCustomizationChangeDTO> changes;
    private Boolean needsClarification;
    private String clarificationReason;
    private String clarificationQuestion;
    private Boolean normalized;
    private String normalizationReason;
    private String requestedValue;
    private String appliedValue;
    private String maxAllowedValue;
    private String minAllowedValue;

    public AiUiCustomizationResponseDTO() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getValuePreset() { return valuePreset; }
    public void setValuePreset(String valuePreset) { this.valuePreset = valuePreset; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public ColorIntentDTO getColorIntent() { return colorIntent; }
    public void setColorIntent(ColorIntentDTO colorIntent) { this.colorIntent = colorIntent; }

    public List<UiCustomizationChangeDTO> getChanges() { return changes; }
    public void setChanges(List<UiCustomizationChangeDTO> changes) { this.changes = changes; }

    public Boolean getNeedsClarification() { return needsClarification; }
    public void setNeedsClarification(Boolean needsClarification) { this.needsClarification = needsClarification; }

    public String getClarificationReason() { return clarificationReason; }
    public void setClarificationReason(String clarificationReason) { this.clarificationReason = clarificationReason; }

    public String getClarificationQuestion() { return clarificationQuestion; }
    public void setClarificationQuestion(String clarificationQuestion) { this.clarificationQuestion = clarificationQuestion; }

    public Boolean getNormalized() { return normalized; }
    public void setNormalized(Boolean normalized) { this.normalized = normalized; }

    public String getNormalizationReason() { return normalizationReason; }
    public void setNormalizationReason(String normalizationReason) { this.normalizationReason = normalizationReason; }

    public String getRequestedValue() { return requestedValue; }
    public void setRequestedValue(String requestedValue) { this.requestedValue = requestedValue; }

    public String getAppliedValue() { return appliedValue; }
    public void setAppliedValue(String appliedValue) { this.appliedValue = appliedValue; }

    public String getMaxAllowedValue() { return maxAllowedValue; }
    public void setMaxAllowedValue(String maxAllowedValue) { this.maxAllowedValue = maxAllowedValue; }

    public String getMinAllowedValue() { return minAllowedValue; }
    public void setMinAllowedValue(String minAllowedValue) { this.minAllowedValue = minAllowedValue; }
}
