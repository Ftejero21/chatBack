package com.chat.chat.Exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Iterator;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MSG_NO_AUTORIZADO = "No autorizado para realizar esta accion";
    private static final String MSG_ERROR_INTERNO = "Error interno del servidor";
    private static final String CODE_ERROR_INTERNO = "ERROR_INTERNO";

    @ExceptionHandler(EmailNoRegistradoException.class)
    public ResponseEntity<ApiError> handleEmail(EmailNoRegistradoException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_EMAIL_INVALIDO, safeMessage(ex.getMessage(), "Email invalido"));
    }

    @ExceptionHandler(PasswordIncorrectaException.class)
    public ResponseEntity<ApiError> handlePass(PasswordIncorrectaException ex) {
        return error(HttpStatus.UNAUTHORIZED, Constantes.ERR_PASSWORD_INCORRECTA, safeMessage(ex.getMessage(), "Credenciales invalidas"));
    }

    @ExceptionHandler(EmailYaExisteException.class)
    public ResponseEntity<ApiError> handleEmailDuplicado(EmailYaExisteException ex) {
        return error(HttpStatus.CONFLICT, "EMAIL_YA_EXISTE", safeMessage(ex.getMessage(), "Conflicto de datos"));
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiError> handleInactivo(UsuarioInactivoException ex) {
        return error(HttpStatus.FORBIDDEN, Constantes.ERR_USUARIO_INACTIVO, safeMessage(ex.getMessage(), "Usuario inactivo"));
    }

    @ExceptionHandler(ReenvioInvalidoException.class)
    public ResponseEntity<ApiError> handleReenvioInvalido(ReenvioInvalidoException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_REENVIO_INVALIDO, safeMessage(ex.getMessage(), "Reenvio invalido"));
    }

    @ExceptionHandler(ReenvioNoAutorizadoException.class)
    public ResponseEntity<ApiError> handleReenvioNoAutorizado(ReenvioNoAutorizadoException ex) {
        return error(HttpStatus.FORBIDDEN, Constantes.ERR_REENVIO_NO_AUTORIZADO, safeMessage(ex.getMessage(), MSG_NO_AUTORIZADO));
    }

    @ExceptionHandler(RespuestaInvalidaException.class)
    public ResponseEntity<ApiError> handleRespuestaInvalida(RespuestaInvalidaException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, safeMessage(ex.getMessage(), Constantes.MSG_FALTAN_DATOS_REQUERIDOS));
    }

    @ExceptionHandler(RespuestaNoAutorizadaException.class)
    public ResponseEntity<ApiError> handleRespuestaNoAutorizada(RespuestaNoAutorizadaException ex) {
        return error(HttpStatus.FORBIDDEN, Constantes.ERR_RESPUESTA_NO_AUTORIZADA, safeMessage(ex.getMessage(), MSG_NO_AUTORIZADO));
    }

    @ExceptionHandler(E2ERekeyConflictException.class)
    public ResponseEntity<ApiError> handleE2ERekeyConflict(E2ERekeyConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getCode(), safeMessage(ex.getMessage(), "Conflicto de estado"));
    }

    @ExceptionHandler(E2EGroupValidationException.class)
    public ResponseEntity<ApiError> handleE2EGroupValidation(E2EGroupValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), safeMessage(ex.getMessage(), Constantes.MSG_FALTAN_DATOS_REQUERIDOS));
    }

    @ExceptionHandler(UploadSecurityException.class)
    public ResponseEntity<ApiError> handleUploadSecurity(UploadSecurityException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), safeMessage(ex.getMessage(), "Archivo invalido o bloqueado"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, Constantes.ERR_NO_AUTORIZADO, MSG_NO_AUTORIZADO);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, Constantes.ERR_NO_ENCONTRADO, safeMessage(ex.getMessage(), "Recurso no encontrado"));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictoException ex) {
        return error(HttpStatus.CONFLICT, ex.getCode(), safeMessage(ex.getMessage(), "Conflicto de estado"));
    }

    @ExceptionHandler(ChatCerradoException.class)
    public ResponseEntity<ApiError> handleChatCerrado(ChatCerradoException ex) {
        return error(HttpStatus.LOCKED, Constantes.ERR_CHAT_CERRADO, safeMessage(ex.getMessage(), "Chat cerrado"));
    }

    @ExceptionHandler(ChatNoCerradoException.class)
    public ResponseEntity<ApiError> handleChatNoCerrado(ChatNoCerradoException ex) {
        return error(HttpStatus.LOCKED, Constantes.ERR_CHAT_NO_CERRADO, safeMessage(ex.getMessage(), "Chat no cerrado"));
    }

    @ExceptionHandler(GoogleAuthException.class)
    public ResponseEntity<ApiError> handleGoogleAuth(GoogleAuthException ex) {
        return error(ex.getStatus(), ex.getCode(), safeMessage(ex.getMessage(), "Error de autenticacion"));
    }

    @ExceptionHandler(SemanticApiException.class)
    public ResponseEntity<ApiError> handleSemanticApiException(SemanticApiException ex) {
        return error(ex.getStatus(), ex.getCode(), safeMessage(ex.getMessage(), "No se pudo procesar la solicitud"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, safeMessage(ex.getMessage(), Constantes.MSG_FALTAN_DATOS_REQUERIDOS));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = Constantes.MSG_FALTAN_DATOS_REQUERIDOS;
        FieldError firstFieldError = ex.getBindingResult().getFieldError();
        if (firstFieldError != null && firstFieldError.getDefaultMessage() != null
                && !firstFieldError.getDefaultMessage().isBlank()) {
            message = firstFieldError.getDefaultMessage();
        }
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, message);
    }

    @ExceptionHandler(ValidacionPayloadException.class)
    public ResponseEntity<ApiError> handleValidacionPayload(ValidacionPayloadException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, safeMessage(ex.getMessage(), Constantes.MSG_FALTAN_DATOS_REQUERIDOS));
    }

    @ExceptionHandler(SqlInjectionException.class)
    public ResponseEntity<ApiError> handleSqlInjection(SqlInjectionException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_SQL_INJECTION, safeMessage(ex.getMessage(), "Entrada invalida"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, resolveConstraintMessage(ex));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, "Formato JSON invalido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, "Parametro requerido ausente: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, Constantes.ERR_RESPUESTA_INVALIDA, "Metodo HTTP no soportado");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ApiError(Constantes.ERR_RATE_LIMIT, safeMessage(ex.getMessage(), "Demasiadas solicitudes")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnhandled(Exception ex, HttpServletRequest request) {
        String path = request == null ? "n/a" : request.getRequestURI();
        LOGGER.error("Unhandled exception path={} type={} message={}", path, ex.getClass().getName(), ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, CODE_ERROR_INTERNO, MSG_ERROR_INTERNO);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

    private String safeMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    private String resolveConstraintMessage(ConstraintViolationException ex) {
        if (ex == null || ex.getConstraintViolations() == null || ex.getConstraintViolations().isEmpty()) {
            return Constantes.MSG_FALTAN_DATOS_REQUERIDOS;
        }
        Iterator<ConstraintViolation<?>> it = ex.getConstraintViolations().iterator();
        if (!it.hasNext()) {
            return Constantes.MSG_FALTAN_DATOS_REQUERIDOS;
        }
        ConstraintViolation<?> violation = it.next();
        if (violation == null || violation.getMessage() == null || violation.getMessage().isBlank()) {
            return Constantes.MSG_FALTAN_DATOS_REQUERIDOS;
        }
        return violation.getMessage();
    }
}
