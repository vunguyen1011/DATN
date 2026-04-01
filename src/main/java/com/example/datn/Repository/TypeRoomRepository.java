package com.example.datn.Repository;

import com.example.datn.Model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TypeRoomRepository extends JpaRepository<RoomType, UUID> {
        boolean existsByName(String name);
        boolean existsByCode(String code);
}
