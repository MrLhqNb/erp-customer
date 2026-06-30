package com.jumbosoft.erpcustomer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<List<ApiResult.FieldError>> handleValidation(MethodArgumentNotValidException e) {
        List<ApiResult.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiResult.FieldError(f.getField(), f.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("Validation error: {} fields", errors.size());
        return ApiResult.fail(400, "Validation failed");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ApiResult.fail(400, "Request body is invalid or missing");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResult<?> handleHttpClientError(HttpClientErrorException e) {
        log.error("Downstream API error: {}", e.getMessage());
        return ApiResult.fail(502, "Downstream service error: " + e.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<?> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResult.fail(500, "Internal server error");
    }
}
