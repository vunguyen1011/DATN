package com.example.datn.Pattern.Stragery;

import com.example.datn.Config.JwtProvider;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.User;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Service.Interface.IRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("GOOGLE")
@RequiredArgsConstructor
@Slf4j
public class GoogleLogin implements ILoginStrategy<OAuth2AuthenticationToken> {

    private final JwtProvider jwtTokenProvider;
    private final IRedisService redisService;
    private final UserRepository userRepository;

    @Value("${jwt.refreshTokenExpiration}")
    private long refreshExpiration;

    @Override
    public TokenResponse authenticate(OAuth2AuthenticationToken requestData, HttpServletResponse response) {
        String email = requestData.getPrincipal().getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            String accessToken = jwtTokenProvider.genAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.genRefreshToken(user.getUsername());

        redisService.saveRefreshToken(
                user.getUsername(),
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
        return "GOOGLE";
    }
}