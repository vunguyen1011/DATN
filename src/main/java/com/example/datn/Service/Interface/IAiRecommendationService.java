package com.example.datn.Service.Interface;

import com.example.datn.DTO.Response.RecommendationResponse;
import java.util.UUID;

public interface IAiRecommendationService {
    RecommendationResponse getRecommendations(UUID studentId);
}
