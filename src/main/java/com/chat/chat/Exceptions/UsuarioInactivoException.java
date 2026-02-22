package com.chat.chat.Exceptions;

public class UsuarioInactivoException extends RuntimeException {
    public UsuarioInactivoException() {
        super("Tu cuenta ha sido inhabilitada por un administrador.");
    }

    public UsuarioInactivoException(String message) {
        super(message);
    }
}
