package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TokenResponse {
    private  String accessToken;

}
