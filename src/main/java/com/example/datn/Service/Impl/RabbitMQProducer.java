package com.example.datn.Service.Impl;

import com.example.datn.Service.Interface.IRabbitMQProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQProducer implements IRabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.name}")
    private String routingKey;



    @Override
    public <T> void sendMessage( T message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
