package com.chat.chat.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailNoRegistradoException.class)
    public ResponseEntity<ApiError> handleEmail(EmailNoRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("EMAIL_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(PasswordIncorrectaException.class)
    public ResponseEntity<ApiError> handlePass(PasswordIncorrectaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("PASSWORD_INCORRECTA", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiError> handleInactivo(UsuarioInactivoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("USUARIO_INACTIVO", ex.getMessage()));
    }
}
