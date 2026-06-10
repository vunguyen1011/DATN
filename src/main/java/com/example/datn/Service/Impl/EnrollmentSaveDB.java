//package com.example.datn.Service.Impl;
//
//import com.example.datn.DTO.Request.EnrollmentSaveRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class EnrollmentSaveDB {
//
//    private final EnrollmentSaveHelper enrollmentSaveHelper;
//
//    @RabbitListener(queues = "${rabbitmq.queue.name}")
//    public void processEnrollmentMessage(List<EnrollmentSaveRequest> requests) {
//        log.info("Received {} enrollment requests from RabbitMQ", requests.size());
//
//        try {
//            enrollmentSaveHelper.saveBatch(requests);
//            log.info("Successfully saved batch to database");
//        } catch (Exception e) {
//            log.error("Failed to save enrollment batch: {}", e.getMessage());
//            throw e;
//        }
//    }
//}
