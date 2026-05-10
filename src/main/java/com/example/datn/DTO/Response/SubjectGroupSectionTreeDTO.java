package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SubjectGroupSectionTreeDTO {
    private String sectionTitle;
    private List<SubjectGradeTreeDTO> subjects;
}
