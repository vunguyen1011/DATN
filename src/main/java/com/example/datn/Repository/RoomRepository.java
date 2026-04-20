package com.example.datn.Repository;

import com.example.datn.Model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    boolean existsByName(String name);

    /**
     * Load Room với PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
     * Dùng trong assignRoom để đảm bảo chỉ 1 transaction được xếp lịch
     * cho một phòng tại một thời điểm — chặn race condition ở application level.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") UUID id);
    Optional<Room> findByName(String  name);
}
