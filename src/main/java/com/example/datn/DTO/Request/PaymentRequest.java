package com.example.datn.DTO.Request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID invoiceId;

    private String bankCode;
}
