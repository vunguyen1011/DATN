package com.example.datn.Pattern.Stragery;

import com.example.datn.Config.JwtProvider;
import com.example.datn.DTO.Request.LoginRequest;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;

import com.example.datn.Service.Interface.IRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
@Component("LOCAL")

@RequiredArgsConstructor
@Slf4j
public class LocalLogin implements ILoginStrategy<LoginRequest> {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtTokenProvider;
    private final IRedisService redisService;

    @Value("${jwt.refreshTokenExpiration}")
    private long refreshExpiration;

    @Override
    public TokenResponse authenticate(LoginRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.authentication.BadCredentialsException |
                 org.springframework.security.authentication.InternalAuthenticationServiceException |
                 org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.warn("Sai thông tin đăng nhập: {}", request.getUsername());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        } catch (DisabledException e) {
            log.warn("Tài khoản chưa được kích hoạt: {}", request.getUsername());
            throw new AppException(ErrorCode.USER_DISABLED);
        } catch (LockedException e) {
            log.warn("Tài khoản bị khóa: {}", request.getUsername());
            throw new AppException(ErrorCode.USER_LOCKED);
        } catch (Exception e) {
            log.error("Lỗi xác thực không xác định: {}", request.getUsername(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.genAccessToken(request.getUsername());
        String refreshToken = jwtTokenProvider.genRefreshToken(request.getUsername());

        redisService.saveRefreshToken(
                request.getUsername(),
                refreshToken,
                Duration.ofSeconds(refreshExpiration)
        );

        ResponseCookie springCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshExpiration)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    @Override
    public String getStrategyName() {
        return "LOCAL";
    }
}