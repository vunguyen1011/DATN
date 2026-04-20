package com.example.datn.Exception;


import com.example.datn.DTO.Response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * Bắt lỗi vi phạm DB constraint (ví dụ: race condition — 2 admin xếp cùng phòng cùng giờ).
     * Thay vì trả về 500 Internal Server Error, trả về 409 Conflict với message thân thiện.
     */
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String message = "Thao tác thất bại do xung đột dữ liệu. "
                + "Phòng học này vừa được xếp bởi người khác, vui lòng thử lại.";
        return ResponseEntity.status(409)
                .body(ApiResponse.builder()
                        .code(1812)
                        .message(message)
                        .build()
                );
    }
}