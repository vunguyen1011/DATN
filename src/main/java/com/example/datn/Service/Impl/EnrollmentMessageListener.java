package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentMessageListener {

    private final EnrollmentSaveHelper enrollmentSaveHelper;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void receiveMessage(List<EnrollmentSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        log.info("Nhận được {} request đăng ký/hủy từ RabbitMQ. Tiến hành lưu DB...", requests.size());
        try {
            enrollmentSaveHelper.saveBatch(requests);
            log.info("Lưu DB qua RabbitMQ thành công!");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                 org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Xung đột dữ liệu khi lưu DB (OptimisticLocking). Đang đẩy lại vào RabbitMQ để Retry: {}", e.getMessage());
            throw e; // Ném Exception để RabbitMQ NACK và tự động retry
        } catch (Exception e) {
            log.error("Lưu DB qua RabbitMQ thất bại: {}", e.getMessage());
            throw e; 
        }
    }
}
