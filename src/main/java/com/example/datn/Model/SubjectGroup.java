    package com.example.datn.Model;

    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.CreationTimestamp;
    import org.hibernate.annotations.UpdateTimestamp;

    import java.time.LocalDateTime;
    import java.util.UUID;

    @Entity
    @Table(name = "subject_groups")
    @Getter // Thay @Data bằng @Getter và @Setter nhé Vũ!
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class SubjectGroup {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "name", length = 255, nullable = false)
        private String name; // VD: "Khối kiến thức Đại cương"


        @Column(name="index")
        private Integer index; // Thứ tự hiển thị trong chương trình học (VD: 1, 2, 3...)


        // --- CỜ ĐÁNH DẤU MODULE DÙNG CHUNG ---
        @Builder.Default
        @Column(name = "is_global")
        private Boolean isGlobal = true; // true: Mặc định dùng chung cho mọi ngành

        // --- QUẢN LÝ TRẠNG THÁI VÀ AUDIT ---
        @Builder.Default
        @Column(name = "is_active")
        private Boolean isActive = true;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
    }