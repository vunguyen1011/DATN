package com.example.datn.Pattern.Stragery;

import com.example.datn.DTO.Response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface ILoginStrategy<T> {
    TokenResponse authenticate(T requestData, HttpServletResponse response);
    String getStrategyName();
}