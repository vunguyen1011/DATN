package com.example.datn.Security;

import com.example.datn.Model.User;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter

public class MyUserDetail implements UserDetails {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private boolean enabled;
    private boolean isLocked;
    private Collection<? extends GrantedAuthority> authorities;
    
    // Additional fields for TPS Level 1 optimization
    @lombok.Setter
    private UUID studentId;
    @lombok.Setter
    private UUID cohortId;

    public MyUserDetail(User user, List<String> roles) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.getIsActive();
        this.isLocked = user.isLocked();
        this.authorities = roles.stream().map(role -> (new SimpleGrantedAuthority(role))).collect(Collectors.toList());
    }

    public MyUserDetail(String username, Collection<? extends GrantedAuthority> authorities, UUID studentId, UUID cohortId) {
        this.username = username;
        this.authorities = authorities;
        this.studentId = studentId;
        this.cohortId = cohortId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }



    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

}
