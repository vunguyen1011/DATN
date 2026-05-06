package com.example.datn.Service.Impl;

import com.example.datn.Config.VNPayConfig;
import com.example.datn.DTO.Request.PaymentRequest;
import com.example.datn.DTO.Response.InvoiceDetailResponse;
import com.example.datn.DTO.Response.InvoiceResponse;
import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.ENUM.InvoiceStatus;
import com.example.datn.ENUM.PaymentMethod;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.ITuitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuitionServiceImpl implements ITuitionService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final StudentRepository studentRepository;

    @Value("${app.tuition.cost-per-credit:450000}")
    private BigDecimal costPerCredit;

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;
    
    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;
    
    @Value("${vnpay.url}")
    private String vnp_PayUrl;
    
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.version}")
    private String vnp_Version;

    @Value("${vnpay.command}")
    private String vnp_Command;

    @Override
    @Transactional
    public int generateInvoicesForRegistrationPeriod(UUID registrationPeriodId) {
        RegistrationPeriod period = registrationPeriodRepository.findById(registrationPeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));

        List<Enrollment> enrollments = enrollmentRepository.findByEnrollmentDateBetweenAndStatus(
                period.getStartTime(), period.getEndTime(), EnrollmentStatus.REGISTERED);
        log.info("Found {} enrollments for registration period {}", enrollments.size(), registrationPeriodId);

        // Group enrollments by student
        Map<Student, List<Enrollment>> studentEnrollmentsMap = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getStudent));

        int invoicesGenerated = 0;

        for (Map.Entry<Student, List<Enrollment>> entry : studentEnrollmentsMap.entrySet()) {
            Student student = entry.getKey();
            List<Enrollment> studentEnrollments = entry.getValue();

            // Check if invoice already exists for this student and semester
            Invoice invoice = invoiceRepository.findByStudentIdAndSemesterId(student.getId(), period.getSemester().getId())
                    .orElse(Invoice.builder()
                            .student(student)
                            .semester(period.getSemester())
                            .totalAmount(BigDecimal.ZERO)
                            .paidAmount(BigDecimal.ZERO)
                            .status(InvoiceStatus.UNPAID)
                            .dueDate(period.getEndTime().plusDays(30)) // Due 30 days after period ends
                            .createdAt(LocalDateTime.now())
                            .build());

            BigDecimal additionalAmount = BigDecimal.ZERO;

            // Kéo ra ngoài vòng lặp của môn học
            List<InvoiceDetail> existingDetails = invoice.getId() != null ? 
                    invoiceDetailRepository.findByInvoiceId(invoice.getId()) : new ArrayList<>();
            Set<UUID> existingSubjectIds = existingDetails.stream()
                    .map(d -> d.getSubject().getId())
                    .collect(Collectors.toSet());

            for (Enrollment enr : studentEnrollments) {
                Subject subject = enr.getClassSection().getSubject();
                
                // Kiểm tra bằng Set trong RAM
                if (!existingSubjectIds.contains(subject.getId())) {
                    BigDecimal subTotal = costPerCredit.multiply(new BigDecimal(subject.getCredits()));
                    additionalAmount = additionalAmount.add(subTotal);
                    
                    // Save invoice first if it's new
                    if (invoice.getId() == null) {
                        invoice.setTotalAmount(additionalAmount);
                        invoice = invoiceRepository.save(invoice);
                        invoicesGenerated++;
                    } else {
                        invoice.setTotalAmount(invoice.getTotalAmount().add(subTotal));
                    }
                    
                    InvoiceDetail detail = InvoiceDetail.builder()
                            .invoice(invoice)
                            .subject(subject)
                            .credits(subject.getCredits())
                            .unitPrice(costPerCredit)
                            .subTotal(subTotal)
                            .build();
                    invoiceDetailRepository.save(detail);
                    
                    // Cập nhật lại Set để tránh trùng lặp
                    existingSubjectIds.add(subject.getId()); 
                }
            }
            
            if (invoice.getId() != null && additionalAmount.compareTo(BigDecimal.ZERO) > 0) {
                invoiceRepository.save(invoice);
            }
        }
        
        return invoicesGenerated;
    }

    @Override
    public List<InvoiceResponse> getMyInvoices(String username) {
        Student student = studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<Invoice> invoices = invoiceRepository.findByStudentId(student.getId());
        return invoices.stream().map(inv -> InvoiceResponse.builder()
                .id(inv.getId())
                .studentId(inv.getStudent().getId())
                .studentName(inv.getStudent().getUser().getFullName())
                .semesterId(inv.getSemester().getId())
                .semesterName(inv.getSemester().getName())
                .totalAmount(inv.getTotalAmount())
                .paidAmount(inv.getPaidAmount())
                .status(inv.getStatus())
                .studentCode(inv.getStudent().getStudentCode())
                .dueDate(inv.getDueDate())
                .createdAt(inv.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDetailResponse> getInvoiceDetails(UUID invoiceId) {
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoiceId);
        return details.stream().map(d -> InvoiceDetailResponse.builder()
                .id(d.getId())
                .invoiceId(d.getInvoice().getId())
                .subjectId(d.getSubject().getId())
                .subjectName(d.getSubject().getName())
                .credits(d.getCredits())
                .unitPrice(d.getUnitPrice())
                .subTotal(d.getSubTotal())
                .build()).collect(Collectors.toList());
    }

    @Override
    public String createVNPayPaymentUrl(PaymentRequest request, String ipAddress) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
                
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        BigDecimal amountToPay = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amountToPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Hóa đơn này đã được thanh toán đủ");
        }
        long amount = amountToPay.longValue() * 100; // VNPay format
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
            vnp_Params.put("vnp_BankCode", request.getBankCode());
        }
        
        String txnRef = VNPayConfig.getRandomNumber(8);
        vnp_Params.put("vnp_TxnRef", txnRef);
        // Put Invoice ID inside OrderInfo to process it later in Return URL
        vnp_Params.put("vnp_OrderInfo", invoice.getId().toString());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(now);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        String vnp_ExpireDate = formatter.format(now.plusMinutes(15));
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        return vnp_PayUrl + "?" + queryUrl;
    }

    @Override
    @Transactional
    public void processVNPayReturn(Map<String, String> vnp_Params) {
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");

        String signValue = VNPayConfig.hashAllFields(vnp_Params, vnp_HashSecret);
        if (!signValue.equals(vnp_SecureHash)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chữ ký không hợp lệ");
        }

        String vnp_ResponseCode = vnp_Params.get("vnp_ResponseCode");
        if ("00".equals(vnp_ResponseCode)) {
            String invoiceIdStr = vnp_Params.get("vnp_OrderInfo");
            String vnp_TransactionNo = vnp_Params.get("vnp_TransactionNo");
            String vnp_TxnRef = vnp_Params.get("vnp_TxnRef");
            BigDecimal amount = new BigDecimal(vnp_Params.get("vnp_Amount"));
            
            UUID invoiceId = UUID.fromString(invoiceIdStr);
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
            
            if (paymentRepository.existsByTransactionCode(vnp_TransactionNo)) {
                return; // Already processed
            }

            BigDecimal paidAmount = amount.divide(new BigDecimal(100)); // VNPay amount is multiplied by 100
            
            Payment payment = Payment.builder()
                    .invoice(invoice)
                    .amount(paidAmount)
                    .transactionCode(vnp_TransactionNo)
                    .paymentMethod(PaymentMethod.VNPAY)
                    .note("VNPay Ref: " + vnp_TxnRef)
                    .build();
            paymentRepository.save(payment);
            
            invoice.setPaidAmount(invoice.getPaidAmount().add(paidAmount));
            if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.PARTIAL);
            }
            invoiceRepository.save(invoice);
        } else {
            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public Page<InvoiceResponse> getMyInvoicesPaged(String username, InvoiceStatus status, int page, int size) {
        Student student = studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Spring Data JPA đếm trang bắt đầu từ 0, nên Client gửi lên page = 1 thì ta phải trừ đi 1
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Invoice> invoicePage = invoiceRepository.findByStudentIdAndStatusWithPagination(
                student.getId(), status, pageable);

        // Map từ Page<Invoice> sang Page<InvoiceResponse>
        return invoicePage.map(inv -> InvoiceResponse.builder()
                .id(inv.getId())
                .studentId(inv.getStudent().getId())
                .studentName(inv.getStudent().getUser().getFullName())
                .semesterId(inv.getSemester().getId())
                .semesterName(inv.getSemester().getName())
                .totalAmount(inv.getTotalAmount())
                .studentCode(inv.getStudent().getStudentCode())
                .paidAmount(inv.getPaidAmount())
                .status(inv.getStatus())
                .dueDate(inv.getDueDate())
                .createdAt(inv.getCreatedAt())
                .build());
    }
    @Override
    public Page<InvoiceResponse> getAllInvoicesForAdmin(InvoiceStatus status, UUID semesterId, int page, int size) {
        // Vẫn trừ đi 1 vì Spring đếm trang từ 0
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Invoice> invoicePage = invoiceRepository.findAllForAdminWithFilters(status, semesterId, pageable);

        return invoicePage.map(inv -> InvoiceResponse.builder()
                .id(inv.getId())
                .studentId(inv.getStudent().getId())
                .studentName(inv.getStudent().getUser().getFullName())
                .studentCode(inv.getStudent().getStudentCode())
                // Bạn có thể bổ sung thêm Mã Sinh Viên (StudentCode) vào DTO nếu FE cần hiển thị
                .semesterId(inv.getSemester().getId())
                .semesterName(inv.getSemester().getName())
                .totalAmount(inv.getTotalAmount())
                .paidAmount(inv.getPaidAmount())
                .status(inv.getStatus())
                .dueDate(inv.getDueDate())
                .createdAt(inv.getCreatedAt())
                .build());
    }
}
