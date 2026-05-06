package com.example.datn.Controller;

import com.example.datn.DTO.Request.PaymentRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.InvoiceDetailResponse;
import com.example.datn.DTO.Response.InvoiceResponse;
import com.example.datn.DTO.Response.VNPayResponse;
import com.example.datn.ENUM.InvoiceStatus;
import com.example.datn.Service.Interface.ITuitionService;
import com.example.datn.Config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

@RestController
@RequestMapping("/api/tuitions")
@RequiredArgsConstructor
public class TuitionController {

    private final ITuitionService tuitionService;

    @PostMapping("/invoices/generate/{registrationPeriodId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Integer>> generateInvoicesForPeriod(@PathVariable UUID registrationPeriodId) {
        int generatedCount = tuitionService.generateInvoicesForRegistrationPeriod(registrationPeriodId);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .code(1000)
                .message("Đã chốt học phí thành công cho đợt đăng ký")
                .result(generatedCount)
                .build());
    }

    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<InvoiceResponse> invoices = tuitionService.getMyInvoices(username);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách hóa đơn thành công")
                .result(invoices)
                .build());
    }

    @GetMapping("/invoices/{invoiceId}/details")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<InvoiceDetailResponse>>> getInvoiceDetails(@PathVariable UUID invoiceId) {
        List<InvoiceDetailResponse> details = tuitionService.getInvoiceDetails(invoiceId);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceDetailResponse>>builder()
                .code(1000)
                .message("Lấy chi tiết hóa đơn thành công")
                .result(details)
                .build());
    }

    @PostMapping("/payments/vnpay")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<VNPayResponse>> createPaymentUrl(@RequestBody PaymentRequest request, HttpServletRequest httpRequest) {
        String ipAddress = VNPayConfig.getIpAddress(httpRequest);
        String paymentUrl = tuitionService.createVNPayPaymentUrl(request, ipAddress);
        
        VNPayResponse response = VNPayResponse.builder()
                .status("OK")
                .message("Tạo đường dẫn thanh toán thành công")
                .paymentUrl(paymentUrl)
                .build();
                
        return ResponseEntity.ok(ApiResponse.<VNPayResponse>builder()
                .code(1000)
                .message("Success")
                .result(response)
                .build());
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(@RequestParam java.util.Map<String, String> params, HttpServletResponse response) throws IOException {
        try {
            tuitionService.processVNPayReturn(params);
            response.sendRedirect("http://localhost:3000/payment/success"); 
        } catch (Exception e) {
            response.sendRedirect("http://localhost:3000/payment/failed");
        }
    }
    @GetMapping("/my-invoices/paged")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getMyInvoicesPaged(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<InvoiceResponse> result = tuitionService.getMyInvoicesPaged(username, status, page, size);

        return ResponseEntity.ok(ApiResponse.<Page<InvoiceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách hóa đơn thành công")
                .result(result)
                .build());
    }
    @GetMapping("/admin/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // Chỉ dành cho Admin/Nhân viên
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoicesForAdmin(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<InvoiceResponse> result = tuitionService.getAllInvoicesForAdmin(status, semesterId, page, size);

        return ResponseEntity.ok(ApiResponse.<Page<InvoiceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách quản lý công nợ thành công")
                .result(result)
                .build());
    }
}
