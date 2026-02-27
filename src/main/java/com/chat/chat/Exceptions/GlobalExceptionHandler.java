package com.chat.chat.Exceptions;

import com.chat.chat.Utils.Constantes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailNoRegistradoException.class)
    public ResponseEntity<ApiError> handleEmail(EmailNoRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Constantes.ERR_EMAIL_INVALIDO, ex.getMessage()));
    }

    @ExceptionHandler(PasswordIncorrectaException.class)
    public ResponseEntity<ApiError> handlePass(PasswordIncorrectaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(Constantes.ERR_PASSWORD_INCORRECTA, ex.getMessage()));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiError> handleInactivo(UsuarioInactivoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Constantes.ERR_USUARIO_INACTIVO, ex.getMessage()));
    }

    @ExceptionHandler(ReenvioInvalidoException.class)
    public ResponseEntity<ApiError> handleReenvioInvalido(ReenvioInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Constantes.ERR_REENVIO_INVALIDO, ex.getMessage()));
    }

    @ExceptionHandler(ReenvioNoAutorizadoException.class)
    public ResponseEntity<ApiError> handleReenvioNoAutorizado(ReenvioNoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Constantes.ERR_REENVIO_NO_AUTORIZADO, ex.getMessage()));
    }

    @ExceptionHandler(RespuestaInvalidaException.class)
    public ResponseEntity<ApiError> handleRespuestaInvalida(RespuestaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Constantes.ERR_RESPUESTA_INVALIDA, ex.getMessage()));
    }

    @ExceptionHandler(RespuestaNoAutorizadaException.class)
    public ResponseEntity<ApiError> handleRespuestaNoAutorizada(RespuestaNoAutorizadaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Constantes.ERR_RESPUESTA_NO_AUTORIZADA, ex.getMessage()));
    }

    @ExceptionHandler(E2ERekeyConflictException.class)
    public ResponseEntity<ApiError> handleE2ERekeyConflict(E2ERekeyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(E2EGroupValidationException.class)
    public ResponseEntity<ApiError> handleE2EGroupValidation(E2EGroupValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Constantes.ERR_NO_AUTORIZADO, ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Constantes.ERR_NO_ENCONTRADO, ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Constantes.ERR_CONFLICTO, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Constantes.ERR_RESPUESTA_INVALIDA, ex.getMessage()));
    }
}
