package com.example.datn.Service.Impl;

import com.example.datn.Model.FailedMessage;
import com.example.datn.Repository.FailedMessageRepository;
import com.example.datn.Service.Interface.IRabbitMQProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQProducer implements IRabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;
    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.name}")
    private String routingKey;

    @PostConstruct
    public void setupCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Tin nhắn không được gửi thành công đến RabbitMQ. Cause: {}", cause);
                if (correlationData instanceof CustomCorrelationData customData) {
                    saveFailedMessage(customData.getPayload());
                }
            } else {
                log.debug("Tin nhắn đã được RabbitMQ nhận (ACK).");
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Tin nhắn bị trả về (Returns). ReplyCode: {}, Exchange: {}",
                    returned.getReplyCode(), returned.getExchange());
            try {
                String payload = new String(returned.getMessage().getBody());
                saveFailedMessage(payload);
            } catch (Exception e) {
                log.error("Lỗi khi parse tin nhắn bị trả về", e);
            }
        });
    }

    @Override
    public <T> void sendMessage(T message) {
        String payload = "";
        try {
            payload = objectMapper.writeValueAsString(message);
            CustomCorrelationData correlationData = new CustomCorrelationData(UUID.randomUUID().toString(), payload);
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
        } catch (Exception e) {
            log.error("Lỗi Exception khi gửi tin nhắn đến RabbitMQ. Kích hoạt Fallback: {}", e.getMessage());
            saveFailedMessage(payload);
        }
    }

    private void saveFailedMessage(String payload) {
        if (payload == null || payload.isEmpty()) return;
        try {
            FailedMessage failedMessage = FailedMessage.builder()
                    .payload(payload)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .status(FailedMessage.MessageStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            failedMessageRepository.save(failedMessage);
            log.info("Đã lưu tin nhắn vào failed_message.");
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Không thể lưu vào failed_message", e);
        }
    }

    public static class CustomCorrelationData extends CorrelationData {
        private final String payload;

        public CustomCorrelationData(String id, String payload) {
            super(id);
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }
}
