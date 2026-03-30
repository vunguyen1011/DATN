package com.example.datn.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Email(message = "Email không hợp lệ")
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "google_subject_id", nullable = true, unique = true)
    private String googleSubjectId;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @NotBlank(message = "Tên đăng nhập (mã sinh viên) không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã sinh viên chỉ được chứa chữ cái và số")
    @Column(nullable = false, unique = true)
    private String username;

    // --- VALIDATE CHO PASSWORD (TỰ SINH) ---
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Column(nullable = false)
    private String password;
    private boolean isLocked = false; // Trường khóa tài khoản


    @Column(name = "microsoft_subject_id", unique = true) // Thêm cho Microsoft
    private String microsoftSubjectId;
}
