package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SubjectGroupTreeDTO {
    private String groupName;
    private List<SubjectGroupSectionTreeDTO> sections;
}
