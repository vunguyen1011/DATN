package com.example.datn.Service.Impl;

import com.example.datn.Config.JwtProvider;
import com.example.datn.DTO.Request.*;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Pattern.Stragery.ILoginStrategy;
import com.example.datn.Repository.LecturerRepository;
import com.example.datn.Repository.RoleRepository;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Repository.UserRoleRepository;
import com.example.datn.Service.Interface.IAuthService;
import com.example.datn.Service.Interface.IEmailService;
import com.example.datn.Service.Interface.ILecturerService;
import com.example.datn.Service.Interface.IRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final Map<String, ILoginStrategy> strategyMap;
    private final JwtProvider jwtTokenProvider;
    private final IRedisService redisService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final LecturerRepository lecturerRepository;

    @Value("${jwt.refreshTokenExpiration}")
    private long refreshExpiration;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @SuppressWarnings("unchecked")
    public <T> TokenResponse executeLogin(String loginMethod, T requestData, HttpServletResponse response) {
        ILoginStrategy<T> strategy = strategyMap.get(loginMethod.toUpperCase());

        if (strategy == null) {
            throw new AppException(ErrorCode.METHOD_NOT_SUPPORTED);
        }

        return strategy.authenticate(requestData, response);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken, HttpServletResponse response) {
        String username;
        try {
            username = jwtTokenProvider.extractUsername(refreshToken);
        } catch (Exception e) {
            log.error("Refresh token error", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String tokenInRedis = redisService.getRefreshToken(username);

        if (tokenInRedis != null && !tokenInRedis.equals(refreshToken)) {
            log.warn("Security warning: Token reuse detected for user: {}", username);
            redisService.deleteRefreshToken(username);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (tokenInRedis == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String newAccessToken = jwtTokenProvider.genAccessToken(username);
        String newRefreshToken = jwtTokenProvider.genRefreshToken(username);

        redisService.saveRefreshToken(
                username,
                newRefreshToken,
                Duration.ofSeconds(refreshExpiration)
        );

        ResponseCookie springCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshExpiration)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Override
    public void logout(String username, HttpServletResponse response) {
        redisService.deleteRefreshToken(username);

        ResponseCookie deleteCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));

        redisService.saveOtp(user.getEmail(), otp);
        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Override
    public String verifyOtp(VerifyOtpRequest request) {
        String savedOtp = redisService.getOtp(request.getEmail());
        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        String resetToken = UUID.randomUUID().toString();
        redisService.deleteOtp(request.getEmail());
        redisService.saveResetToken(request.getEmail(), resetToken, Duration.ofMinutes(10));

        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        String savedToken = redisService.getResetToken(request.getEmail());
        if (savedToken == null || !savedToken.equals(request.getResetToken())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisService.deleteResetToken(request.getEmail());
    }

    @Override
    @Transactional // BẮT BUỘC PHẢI CÓ
    public void assignRoleToUser(String username) { // Đổi tên biến request -> username cho dễ đọc

        // 1. Tìm User sắp được thăng chức
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Tìm Role Trưởng phòng
        Role role = roleRepository.findByName("ROLE_HOD")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // 3. Tìm Ngành (Major) của User này thông qua bảng Lecturer
        Lecturer lecturer = lecturerRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy hồ sơ Giảng viên của người dùng này"));
        Major major = lecturer.getMajor();

        // 4. Kiểm tra xem người này có đang làm Trưởng phòng sẵn rồi không
        boolean alreadyHasRole = userRoleRepository.existsByUserAndRole(user, role);
        if (alreadyHasRole) {
            throw new AppException(ErrorCode.USER_ALREADY_HAS_ROLE);
        }

        // 5. BƯỚC QUAN TRỌNG: Xóa quyền Trưởng phòng của Thầy/Cô cũ (Cùng Ngành)
        List<UserRole> oldHodRoles = userRoleRepository.findOldHodByRoleAndMajor(role, major);
        if (!oldHodRoles.isEmpty()) {
            userRoleRepository.deleteAll(oldHodRoles); // Thu hồi quyền
        }

        // 6. Gán Role HOD cho Thầy/Cô mới
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();

        userRoleRepository.save(userRole);

        log.info("Đã bổ nhiệm user {} làm Trưởng phòng mới cho Ngành {}", user.getUsername(), major.getId());
    }
}