package com.example.datn.Config;

import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Role;
import com.example.datn.Model.User;
import com.example.datn.Model.UserRole;
import com.example.datn.Repository.RoleRepository;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!roleRepository.existsByName("ROLE_USER")) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        }
        if (!roleRepository.existsByName("ROLE_LECTURER")) {
            roleRepository.save(Role.builder().name("ROLE_LECTURER").build());
        }
        if (!roleRepository.existsByName("ROLE_HOD")) {
            roleRepository.save(Role.builder().name("HOD").build());
        }

        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .email("admin@system.com")
                    .fullName("System Administrator")
                    .isActive(true)
                    .build();

            User savedAdmin = userRepository.save(admin);

            UserRole userRole = UserRole.builder()
                    .user(savedAdmin)
                    .role(adminRole)
                    .build();

            userRoleRepository.save(userRole);
        }
    }
}