package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Service.Impl.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final SupportService supportService;
    @GetMapping("/hello")
    public String quickTest() {
        return "Server is running!";
    }
    @PostMapping("/create-token-csv")
    public ResponseEntity<Void> createTokenCsv() {
        supportService.createTokenCsvFile();
        return ResponseEntity.ok().build();
    }
}
