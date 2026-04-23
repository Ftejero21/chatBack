package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;

public class UpdatePublicKeyRequestDTO {

    @NotBlank(message = "publicKey es obligatoria")
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
