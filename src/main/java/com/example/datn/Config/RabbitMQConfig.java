package com.example.datn.Config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Value("${rabbitmq.queue.name}")
    private String queue;
    @Value("${rabbitmq.exchange.name}")
    private String exchange;
    @Value("${rabbitmq.routing.name}")
    private String routingKey;

    public static final String DLQ = "enrollment_dlq";
    public static final String DLX = "enrollment_dlx";
    public static final String DLQ_ROUTING_KEY = "enrollment_dlq_routing_key";

    @Bean
    public Queue dlq() {
        return org.springframework.amqp.core.QueueBuilder.durable(DLQ).quorum().build();
    }

    @Bean
    public TopicExchange dlx() {
        return new TopicExchange(DLX);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlx()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue queue(){
        return org.springframework.amqp.core.QueueBuilder.durable(queue)
                .quorum()
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public TopicExchange exchange(){
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding binding(){
       return BindingBuilder.bind(queue()).to(exchange()).with(routingKey);
    }
    @Bean
    public Jackson2JsonMessageConverter messageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper typeMapper = 
                new org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

}
