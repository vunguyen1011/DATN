package com.example.datn.Repository;

import com.example.datn.Model.Major;
import com.example.datn.Model.Role;
import com.example.datn.Model.User;
import com.example.datn.Model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole(User user, Role role);
    List<UserRole> findByRole(Role role);
    @Query("SELECT ur FROM UserRole ur WHERE ur.role = :role AND ur.user.id IN (SELECT l.user.id FROM Lecturer l WHERE l.major = :major)")
    List<UserRole> findOldHodByRoleAndMajor(@Param("role") Role role, @Param("major") Major major);

}
