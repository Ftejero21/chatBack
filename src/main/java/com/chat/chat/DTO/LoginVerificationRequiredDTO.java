package com.chat.chat.DTO;

public class LoginVerificationRequiredDTO {
    private boolean requiresVerification;
    private String flow;
    private String mensaje;

    public LoginVerificationRequiredDTO() {
    }

    public LoginVerificationRequiredDTO(boolean requiresVerification, String flow, String mensaje) {
        this.requiresVerification = requiresVerification;
        this.flow = flow;
        this.mensaje = mensaje;
    }

    public boolean isRequiresVerification() {
        return requiresVerification;
    }

    public void setRequiresVerification(boolean requiresVerification) {
        this.requiresVerification = requiresVerification;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
