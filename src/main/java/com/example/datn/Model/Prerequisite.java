package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(name = "prerequisites", indexes = {
        @Index(name = "idx_prerequisite_subject", columnList = "subject_id")
})

public class Prerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;
    @ManyToOne @JoinColumn(name = "prerequisite_id")
    private Subject prerequisiteSubject;
}
