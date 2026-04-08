package com.example.datn.Repository;

import com.example.datn.Model.Role;
import com.example.datn.Model.User;
import com.example.datn.Model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole(User user, Role role);
    List<UserRole> findByRole(Role role);

}
