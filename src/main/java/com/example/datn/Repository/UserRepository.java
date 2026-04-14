package com.example.datn.Repository;

import com.example.datn.Model.Major;
import com.example.datn.Model.Role;
import com.example.datn.Model.User;
import com.example.datn.Model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u.username FROM User u WHERE u.username IN :usernames")
    List<String> findUsernamesByUsernameIn(@Param("usernames") List<String> usernames);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    @Query("SELECT ur.user FROM UserRole ur WHERE ur.role.name = :roleName AND ur.user.isActive = true")
    Page<User> findByRoleNameAndIsActiveTrue(@Param("roleName") String roleName, Pageable pageable);
    @Query("SELECT u FROM Lecturer l JOIN l.user u JOIN UserRole ur ON ur.user.id = u.id WHERE l.major.id = :majorId AND ur.role.name = 'ROLE_HEAD_OF_MAJOR' AND u.isActive = true")
    Optional<User> findHeadOfMajorByMajorId(@Param("majorId") UUID majorId);
    @Query("SELECT ur FROM UserRole ur WHERE ur.role = :role AND ur.user.id IN (SELECT l.user.id FROM Lecturer l WHERE l.major = :major)")
    List<UserRole> findOldHodByRoleAndMajor(@Param("role") Role role, @Param("major") Major major);

}
