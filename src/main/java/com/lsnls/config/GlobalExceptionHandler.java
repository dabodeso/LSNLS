package com.lsnls.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatus().value();
        String mensaje = MensajesUsuario.sanitizar(ex.getReason(), MensajesUsuario.porHttp(status));
        return cuerpo(ex.getStatus(), mensaje);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return cuerpo(HttpStatus.PAYLOAD_TOO_LARGE, MensajesUsuario.ARCHIVO_GRANDE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return cuerpo(HttpStatus.FORBIDDEN, MensajesUsuario.PERMISOS);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return cuerpo(HttpStatus.UNAUTHORIZED, MensajesUsuario.SESION);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Conflicto de edición concurrente: {}", ex.getMessage());
        return cuerpo(HttpStatus.CONFLICT, MensajesUsuario.CONCURRENCIA);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return cuerpo(HttpStatus.CONFLICT, MensajesUsuario.RELACIONADOS);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de petición no legible: {}", ex.getMostSpecificCause().getMessage());
        return cuerpo(HttpStatus.BAD_REQUEST, MensajesUsuario.VALIDACION);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .collect(Collectors.joining(". "));
        return cuerpo(HttpStatus.BAD_REQUEST, MensajesUsuario.sanitizar(detalle, MensajesUsuario.VALIDACION));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraint(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(". "));
        return cuerpo(HttpStatus.BAD_REQUEST, MensajesUsuario.sanitizar(detalle, MensajesUsuario.VALIDACION));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, MensajesUsuario.sanitizar(ex.getMessage(), MensajesUsuario.VALIDACION));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return cuerpo(HttpStatus.CONFLICT, MensajesUsuario.sanitizar(ex.getMessage(), MensajesUsuario.GENERICO));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, MensajesUsuario.GENERICO);
    }

    private static ResponseEntity<Map<String, String>> cuerpo(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(
                "mensaje", mensaje,
                "message", mensaje
        ));
    }
}
