package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.ENUM.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FastBatchProcessor {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeFastBatch(List<EnrollmentSaveRequest> requests) {
        Map<UUID, Long> sectionDeltas = requests.stream()
                .collect(Collectors.groupingBy(
                        EnrollmentSaveRequest::classSectionId,
                        Collectors.summingLong(this::calculateDelta)
                ));

        String updateSql = "UPDATE class_sections SET enrolled_count = enrolled_count + ? " +
                "WHERE id = ? AND enrolled_count + ? >= 0 AND enrolled_count + ? <= capacity";

        List<Object[]> updateArgs = sectionDeltas.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .map(entry -> new Object[]{entry.getValue(), entry.getKey(), entry.getValue(), entry.getValue()})
                .collect(Collectors.toList());

        if (!updateArgs.isEmpty()) {
            int[] updateResults = jdbcTemplate.batchUpdate(updateSql, updateArgs);
            for (int res : updateResults) {
                if (res == 0) {
                    throw new RuntimeException("DB Rejection: Capacity limit exceeded or invalid count");
                }
            }
        }

        // Tách ra Delete và Upsert
        List<EnrollmentSaveRequest> upsertReqs = requests.stream()
                .filter(req -> req.newStatus() != EnrollmentStatus.CANCELLED)
                .collect(Collectors.toList());
        
        List<EnrollmentSaveRequest> deleteReqs = requests.stream()
                .filter(req -> req.newStatus() == EnrollmentStatus.CANCELLED)
                .collect(Collectors.toList());

        if (!deleteReqs.isEmpty()) {
            String deleteSql = "DELETE FROM enrollments WHERE student_id = ? AND class_section_id = ?";
            List<Object[]> deleteArgs = deleteReqs.stream()
                    .map(req -> new Object[]{req.studentId(), req.classSectionId()})
                    .collect(Collectors.toList());
            jdbcTemplate.batchUpdate(deleteSql, deleteArgs);
        }

        if (!upsertReqs.isEmpty()) {
            String upsertSql = "INSERT INTO enrollments (id, student_id, class_section_id, status, enrollment_date) " +
                    "VALUES (?, ?, ?, ?::varchar, ?) " +
                    "ON CONFLICT (student_id, class_section_id) " +
                    "DO UPDATE SET status = EXCLUDED.status, enrollment_date = EXCLUDED.enrollment_date";

            List<Object[]> upsertArgs = upsertReqs.stream()
                    .map(req -> new Object[]{
                            req.enrollmentId(), req.studentId(), req.classSectionId(),
                            req.newStatus().name(), req.enrollmentDate()
                    })
                    .collect(Collectors.toList());

            jdbcTemplate.batchUpdate(upsertSql, upsertArgs);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeSingleSave(EnrollmentSaveRequest req) {
        this.executeFastBatch(List.of(req));
    }

    private long calculateDelta(EnrollmentSaveRequest req) {
        long delta = 0;
        if (req.newStatus() == EnrollmentStatus.REGISTERED) delta += 1;
        if (req.oldStatus() == EnrollmentStatus.REGISTERED) delta -= 1;
        return delta;
    }
}
