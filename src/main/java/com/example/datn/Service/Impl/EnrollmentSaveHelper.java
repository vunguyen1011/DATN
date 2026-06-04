package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentSaveHelper {

    private final FastBatchProcessor fastBatchProcessor;

    public void saveBatch(List<EnrollmentSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        List<EnrollmentSaveRequest> mutableRequests = new ArrayList<>(requests);
        mutableRequests.sort(Comparator.comparing(EnrollmentSaveRequest::classSectionId));

        try {
            fastBatchProcessor.executeFastBatch(mutableRequests);
        } catch (Exception e) {
            log.warn("Fast batch failed: {}. Executing fallback rescue.", e.getMessage());
            safeFallbackRescue(mutableRequests);
        }
    }

    public void saveOne(EnrollmentSaveRequest req) {
        try {
            fastBatchProcessor.executeSingleSave(req);
        } catch (Exception ex) {
            log.error("Data sync failed for student {} in class {}. Error: {}",
                    req.studentId(), req.classSectionId(), ex.getMessage());
            throw ex;
        }
    }

    private void safeFallbackRescue(List<EnrollmentSaveRequest> requests) {
        for (EnrollmentSaveRequest req : requests) {
            try {
                fastBatchProcessor.executeSingleSave(req);
            } catch (Exception ex) {
                log.error("Data sync failed for student {} in class {}. Error: {}",
                        req.studentId(), req.classSectionId(), ex.getMessage());
            }
        }
    }
}