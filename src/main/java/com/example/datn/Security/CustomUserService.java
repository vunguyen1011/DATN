package com.example.datn.Security;

import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.User;
import com.example.datn.Model.UserRole;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<UserRole> userRoles  = userRoleRepository.findByUser(user);
        List<String> roleNames = userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
        return new MyUserDetail(user, roleNames);
    }
}