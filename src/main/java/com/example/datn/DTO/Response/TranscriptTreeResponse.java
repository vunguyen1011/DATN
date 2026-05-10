package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TranscriptTreeResponse {
    private UUID programId;
    private String programCode;
    private String programName;
    private List<SubjectGroupTreeDTO> subjectGroups;
}
