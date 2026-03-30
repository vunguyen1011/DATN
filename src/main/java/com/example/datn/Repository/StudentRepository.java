package com.example.datn.Repository;

import com.example.datn.Model.Student;
import com.example.datn.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.SimpleTimeZone;
import java.util.UUID;
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByUser(User user);
}
