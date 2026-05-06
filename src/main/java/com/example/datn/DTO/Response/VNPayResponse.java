package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VNPayResponse {
    private String status;
    private String message;
    private String paymentUrl;
}
