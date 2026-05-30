package com.example.datn.Service.Interface;


import java.util.List;

public interface IRabbitMQProducer {
    <T> void sendMessage( T message);}