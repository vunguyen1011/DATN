package com.example.datn.Repository;

import com.example.datn.Model.ProgramCohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramCohortRepository extends JpaRepository<ProgramCohort, UUID> {

    boolean existsByProgramIdAndCohortId(UUID programId, UUID cohortId);

    Optional<ProgramCohort> findByProgramIdAndCohortId(UUID programId, UUID cohortId);

    List<ProgramCohort> findByProgramId(UUID programId);

    List<ProgramCohort> findByCohortId(UUID cohortId);

    @Query("SELECT pc FROM ProgramCohort pc JOIN FETCH pc.cohort WHERE pc.program.id = :programId")
    List<ProgramCohort> findByProgramIdFetchCohort(@Param("programId") UUID programId);

    @Query("SELECT pc FROM ProgramCohort pc JOIN FETCH pc.program WHERE pc.cohort.id = :cohortId")
    List<ProgramCohort> findByCohortIdFetchProgram(@Param("cohortId") UUID cohortId);
    Boolean existsByCohortId(UUID cohortId);
    Boolean existsByProgramId(UUID programId);
}
