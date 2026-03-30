package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PrerequisiteRequest {
    @NotEmpty(message = "Danh sách môn tiên quyết không được để trống")
    private List<UUID> prerequisiteIds;
}