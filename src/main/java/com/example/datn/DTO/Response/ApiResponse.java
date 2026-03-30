package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

/**
 * @param <T>
 * @author Admin
 */
@Builder
@Data
public class ApiResponse<T> {
    @Builder.Default
    private int code = 200;
    private String message;
    private T result;
}