package com.restaurante.shared.web;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.DomainException;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.domain.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones del sistema a {@link ProblemDetail} (RFC 7807).
 * Los mensajes internos jamás se exponen en errores 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROBLEM_BASE = "urn:problem:restaurante:";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "no-encontrado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "no-autenticado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "conflicto", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "dominio", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return buildValidationErrors(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBind(BindException ex, HttpServletRequest request) {
        return buildValidationErrors(ex, request);
    }

    private ResponseEntity<ProblemDetail> buildValidationErrors(org.springframework.validation.BindingResult bindingResult,
                                                                 HttpServletRequest request) {
        Map<String, String> fieldErrors = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "inválido" : error.getDefaultMessage(),
                        (a, b) -> a));
        Map<String, Object> properties = fieldErrors.isEmpty() ? null : Map.of("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "validacion", "La solicitud contiene campos inválidos", request, properties);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleJavaxConstraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (a, b) -> a));
        return build(HttpStatus.BAD_REQUEST, "validacion", "La solicitud contiene campos inválidos",
                request, Map.of("fieldErrors", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "cuerpo-invalido", ex.getMostSpecificCause().getMessage(), request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "parametro-faltante", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "El parámetro '" + ex.getName() + "' tiene un tipo inválido";
        return build(HttpStatus.BAD_REQUEST, "parametro-invalido", message, request, null);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> handleIntegrity(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de integridad o concurrencia: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "conflicto-datos", "La operación entró en conflicto con los datos actuales", request, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "acceso-denegado", "No tienes permiso para esta operación", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} [{}]", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Ocurrió un error inesperado", request, null);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String type, String detail,
                                                 HttpServletRequest request, Map<String, Object> properties) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + type));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", request.getRequestURI());
        if (properties != null) {
            properties.forEach(problem::setProperty);
        }
        return ResponseEntity.status(status).body(problem);
    }
}