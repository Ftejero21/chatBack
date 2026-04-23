package com.chat.chat.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordRecoveryVerifyRequestDTO {

    @NotBlank(message = "email es obligatorio")
    @Email(message = "email invalido")
    @Size(max = 190, message = "email invalido")
    private String email;

    @NotBlank(message = "code es obligatorio")
    @Size(max = 128, message = "code invalido")
    private String code;

    @NotBlank(message = "newPassword es obligatorio")
    @Size(max = 255, message = "newPassword invalido")
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
