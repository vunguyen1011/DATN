package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class InvoiceDetailResponse {
    private UUID id;
    private UUID invoiceId;
    private UUID subjectId;
    private String subjectName;
    private Integer credits;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
