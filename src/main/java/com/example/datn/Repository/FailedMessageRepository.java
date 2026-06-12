package com.example.datn.Repository;

import com.example.datn.Model.FailedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, UUID> {
    List<FailedMessage> findByStatusOrderByCreatedAtAsc(FailedMessage.MessageStatus status);
}
