package com.example.datn.Task;

import com.example.datn.Model.FailedMessage;
import com.example.datn.Repository.FailedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedMessageRetryJob {

    private final FailedMessageRepository failedMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {
        List<FailedMessage> pendingMessages = failedMessageRepository.findByStatusOrderByCreatedAtAsc(FailedMessage.MessageStatus.PENDING);
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Bắt đầu retry {} tin nhắn bị lỗi...", pendingMessages.size());

        for (FailedMessage message : pendingMessages) {
            try {
                com.example.datn.DTO.Request.EnrollmentSaveRequest[] requests = objectMapper.readValue(
                        message.getPayload(),
                        com.example.datn.DTO.Request.EnrollmentSaveRequest[].class
                );

                com.example.datn.Service.Impl.RabbitMQProducer.CustomCorrelationData correlationData =
                        new com.example.datn.Service.Impl.RabbitMQProducer.CustomCorrelationData(message.getId().toString(), message.getPayload());

                rabbitTemplate.convertAndSend(message.getExchange(), message.getRoutingKey(), requests, correlationData);

                message.setStatus(FailedMessage.MessageStatus.PROCESSED);
                failedMessageRepository.save(message);

                log.info("Gửi lại thành công tin nhắn id: {}", message.getId());
            } catch (Exception e) {
                log.error("Gửi lại tin nhắn thất bại id: {}", message.getId(), e);
            }
        }
    }
}
