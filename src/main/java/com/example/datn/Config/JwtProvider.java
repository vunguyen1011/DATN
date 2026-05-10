package com.example.datn.Config;

import com.example.datn.Security.CustomUserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Model.Student;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter

public class JwtProvider {
    private final CustomUserService userService;
    private final StudentRepository studentRepository;

    @Value("${jwt.signerKey}")
    private String secretKey;

    @Value("${jwt.refreshTokenExpiration}")
    private long refreshExpiration;

    @Value("${jwt.accessTokenExpiration}")
    private long jwtExpiration;
    private Key getKey(){
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String genToken(String username,long timeExpiration){
        UserDetails user=userService.loadUserByUsername(username);
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
                
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuer("NguyenVu")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+timeExpiration))
                .claim("roles",roles);
                
        if (roles.contains("ROLE_USER") || roles.contains("USER")) {
            Optional<Student> studentOpt = studentRepository.findByUser_Username(username);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                builder.claim("studentId", student.getId().toString());
                builder.claim("cohortId", student.getCohort().getId().toString());
            }
        }
        
        return builder.signWith(getKey()).compact();
    }
    public String genAccessToken(String username){
        return genToken(username,jwtExpiration);
    }
    public String genRefreshToken(String username){
        return genToken(username,refreshExpiration);
    }
    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        Claims claims=extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    public String extractClaimAsString(String token, String claimKey) {
        Claims claims = extractAllClaims(token);
        return claims.get(claimKey, String.class);
    }
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        }
        return false;
    }
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
    public List<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            return new ArrayList<>(); // Nếu không có quyền gì thì trả về list rỗng
        }
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}
