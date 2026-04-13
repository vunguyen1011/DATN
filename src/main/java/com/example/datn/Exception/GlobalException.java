package com.example.datn.Exception;


import com.example.datn.DTO.Response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlerRunTimeException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.badRequest()
                .body(ApiResponse.builder().code(errorCode.getCode()).message(exception.getMessage())
                        .build()
                );
    }
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(ApiResponse.builder().code(400).message(errorMessage)
                        .build()
                );
    }
    @ExceptionHandler(value = org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiResponse> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException exception) {
        return ResponseEntity.status(403)
                .body(ApiResponse.builder()
                        .code(403)
                        .message("Bạn không có quyền truy cập tài nguyên này")
                        .build()
                );
    }
}