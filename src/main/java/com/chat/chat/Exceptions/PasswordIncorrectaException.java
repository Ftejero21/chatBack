package com.chat.chat.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class PasswordIncorrectaException extends RuntimeException {
    public PasswordIncorrectaException() { super("Contraseña incorrecta"); }
}