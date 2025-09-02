package com.dropslot.user.api;

import com.dropslot.user.api.dto.ProblemDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDto> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest req) {
    // Treat as client error
    log.info("Bad request: {}", ex.getMessage());
    ProblemDto p =
        new ProblemDto(
            "about:blank",
            "Bad Request",
            ex.getMessage(),
            req.getRequestURI(),
            HttpStatus.BAD_REQUEST.value(),
            Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(p);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDto> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    ProblemDto p =
        new ProblemDto(
            "about:blank",
            "Unauthorized",
            ex.getMessage(),
            req.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            Instant.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(p);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDto> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    ProblemDto p =
        new ProblemDto(
            "about:blank",
            "Forbidden",
            ex.getMessage(),
            req.getRequestURI(),
            HttpStatus.FORBIDDEN.value(),
            Instant.now());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(p);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDto> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    ProblemDto p =
        new ProblemDto(
            "about:blank",
            "Validation Failed",
            details,
            req.getRequestURI(),
            HttpStatus.BAD_REQUEST.value(),
            Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(p);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDto> handleGeneric(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception", ex);
    ProblemDto p =
        new ProblemDto(
            "about:blank",
            "Internal Server Error",
            ex.getMessage(),
            req.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            Instant.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(p);
  }
}
