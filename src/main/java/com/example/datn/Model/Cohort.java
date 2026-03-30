package com.example.datn.Model;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity
@Table(name = "cohorts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(name = "start_year", nullable = false)
    private Integer startYear;

}