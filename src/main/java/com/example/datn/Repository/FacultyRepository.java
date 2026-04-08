package com.example.datn.Repository;

import com.example.datn.Model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FacultyRepository  extends JpaRepository<Faculty, UUID> {
}
