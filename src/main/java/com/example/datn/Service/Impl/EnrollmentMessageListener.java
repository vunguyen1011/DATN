package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Service.Interface.IRedisService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentMessageListener {

    private final EnrollmentSaveHelper enrollmentSaveHelper;
    private final IRedisService redisService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void receiveMessage(EnrollmentSaveRequest[] requestArray, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (requestArray == null || requestArray.length == 0) {
            channel.basicAck(tag, false);
            return;
        }
        List<EnrollmentSaveRequest> requests = java.util.Arrays.asList(requestArray);
        log.info("Nhận được {} request đăng ký/hủy từ RabbitMQ. Tiến hành lưu DB...", requests.size());
        try {
            enrollmentSaveHelper.saveBatch(requests);
            log.info("Lưu DB qua RabbitMQ thành công!");

            // Two-Stage TTL: Giảm tuổi thọ của Pending Buffer xuống 2 giây sau khi lưu DB thành công
            // Giao phó hoàn toàn việc đếm ngược và dọn dẹp cho Redis (True Stateless)
            for (EnrollmentSaveRequest req : requests) {
                if (req.newStatus() == EnrollmentStatus.REGISTERED) {
                    redisService.expirePendingRegistration(req.studentId(), req.classSectionId(), Duration.ofSeconds(2));
                } else if (req.newStatus() == EnrollmentStatus.CANCELLED) {
                    redisService.expirePendingCancellation(req.studentId(), req.classSectionId(), Duration.ofSeconds(2));
                }
            }

            channel.basicAck(tag, false);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException
                | org.springframework.dao.CannotAcquireLockException e) {
            log.error("Xung đột dữ liệu hoặc Lock DB (Transient). Requeue để thử lại: {}", e.getMessage());
            // Lỗi do timing/lock DB, cho phép requeue để thử lại
            channel.basicNack(tag, false, true);
        } catch (Exception e) {
            log.error("Lỗi Logic không thể phục hồi (Poison Pill). Đẩy sang DLQ: {}", e.getMessage());
            // Lỗi logic (NPE, DataIntegrity do sai constraint), KHÔNG requeue -> RabbitMQ
            // sẽ tự đẩy sang DLX/DLQ
            channel.basicNack(tag, false, false);
        }
    }
}
