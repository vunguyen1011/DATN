package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "majors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name; // VD: Kỹ thuật phần mềm
    @Column(nullable = false, unique = true)
    private String code; // VD: SE (Software Engineering)
    @Builder.Default
    private Boolean isActive = true;

}