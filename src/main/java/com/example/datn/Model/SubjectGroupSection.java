package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subject_group_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectGroupSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "index")
    private Integer index;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory;

    @Column(name = "required_credits", nullable = false)
    private Integer requiredCredits;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_program_id", nullable = false)
    private EducationProgram educationProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_group_id", nullable = false)
    private SubjectGroup subjectGroup;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}