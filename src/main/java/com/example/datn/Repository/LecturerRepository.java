package com.example.datn.Repository;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LecturerRepository  extends JpaRepository<Lecturer, UUID> {
    Optional<Lecturer> findByUser(User user);
}
