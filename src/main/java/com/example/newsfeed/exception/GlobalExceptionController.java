package com.example.newsfeed.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionController {

    //커스텀
    @ExceptionHandler
    public ResponseEntity<String> duplicatedException(DuplicatedException e) {
        return new ResponseEntity<>(e.getErrorCode().getMessage(), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<String> internalServerException(InternalServerException e) {
        return new ResponseEntity<>(e.getErrorCode().getMessage(), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<String> invalidInputException(InvalidInputException e) {
        return new ResponseEntity<>(e.getErrorCode().getMessage(), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<String> notFoundException(NotFoundException e) {
        return new ResponseEntity<>(e.getErrorCode().getMessage(), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<String> noAuthorizedException(NoAuthorizedException e) {
        return new ResponseEntity<>(e.getErrorCode().getMessage(), e.getErrorCode().getHttpStatus());
    }

    //자바

    @ExceptionHandler
    public ResponseEntity<String> constrainViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolation: {}", e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        // Keep only the first message per field so the client can show it inline.
        Map<String, String> perField = fieldErrors.stream().collect(Collectors.toMap(
                FieldError::getField,
                fe -> {
                    String m = fe.getDefaultMessage();
                    return m == null ? "유효하지 않은 값입니다." : m;
                },
                (first, second) -> first,
                LinkedHashMap::new
        ));

        String message = perField.values().stream().findFirst().orElseGet(() ->
                e.getBindingResult().getAllErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .findFirst().orElse("입력 값이 유효하지 않습니다."));

        log.warn("Validation failed: {} | fields={}", message, perField);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("fieldErrors", perField);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> constraintViolationException(Exception e) {
        log.error("Unhandled exception → returning 400: {}", e.getMessage(), e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
