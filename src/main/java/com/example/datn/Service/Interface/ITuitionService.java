package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.PaymentRequest;
import com.example.datn.DTO.Response.InvoiceDetailResponse;
import com.example.datn.DTO.Response.InvoiceResponse;
import com.example.datn.ENUM.InvoiceStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ITuitionService {
    int generateInvoicesForRegistrationPeriod(UUID registrationPeriodId);
    List<InvoiceResponse> getMyInvoices(String username);
    List<InvoiceDetailResponse> getInvoiceDetails(UUID invoiceId);
    String createVNPayPaymentUrl(PaymentRequest request, String ipAddress);
    void processVNPayReturn(java.util.Map<String, String> vnp_Params);
    Page<InvoiceResponse> getMyInvoicesPaged(String username, InvoiceStatus status, int page, int size);
    Page<InvoiceResponse> getAllInvoicesForAdmin(InvoiceStatus status, UUID semesterId, int page, int size);
}
