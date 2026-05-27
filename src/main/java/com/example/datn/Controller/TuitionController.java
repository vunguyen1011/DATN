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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

@Tag(name = "Tuition / Invoices / Payments", description = "Quản lý học phí, xuất hóa đơn và thanh toán qua VNPay")
@RestController
@RequestMapping("/api/tuitions")
@RequiredArgsConstructor
public class TuitionController {

    private final ITuitionService tuitionService;

    @Operation(summary = "Tạo hóa đơn học phí cho sinh viên trong đợt đăng ký")
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

    @Operation(summary = "Lấy danh sách hóa đơn học phí của sinh viên đang đăng nhập")
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

    @Operation(summary = "Lấy chi tiết hóa đơn học phí")
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

    @Operation(summary = "Tạo link thanh toán VNPay cho hóa đơn")
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

    @Operation(summary = "Nhận kết quả trả về từ cổng thanh toán VNPay")
    @GetMapping("/vnpay-return")
    public void vnpayReturn(@RequestParam java.util.Map<String, String> params, HttpServletResponse response) throws IOException {
        try {
            tuitionService.processVNPayReturn(params);
            response.sendRedirect("https://register-for-study-fczn.vercel.app/payment-success");
        } catch (Exception e) {
            response.sendRedirect("https://register-for-study-fczn.vercel.app/payment-failed");
        }
    }
    @Operation(summary = "Lấy danh sách hóa đơn học phí có phân trang")
    @GetMapping("/my-invoices/paged")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getMyInvoicesPaged(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<InvoiceResponse> result = tuitionService.getMyInvoicesPaged(username, status, page, size);

        return ResponseEntity.ok(ApiResponse.<Page<InvoiceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách hóa đơn thành công")
                .result(result)
                .build());
    }
    @Operation(summary = "Admin lấy danh sách tất cả hóa đơn học phí trong hệ thống (phân trang)")
    @GetMapping("/admin/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // Chỉ dành cho Admin/Nhân viên
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoicesForAdmin(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<InvoiceResponse> result = tuitionService.getAllInvoicesForAdmin(status, semesterId, page, size);

        return ResponseEntity.ok(ApiResponse.<Page<InvoiceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách quản lý công nợ thành công")
                .result(result)
                .build());
    }
}
