package com.example.datn.DTO.Response;

import com.example.datn.ENUM.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private UUID semesterId;
    private String semesterName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private InvoiceStatus status;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
}
