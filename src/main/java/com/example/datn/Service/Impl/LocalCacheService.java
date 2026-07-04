package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.ClassSectionCacheDTO;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LocalCacheService {
    // Cache tĩnh lưu trên RAM JVM
    private final ConcurrentHashMap<UUID, ClassSectionCacheDTO> classMetadataCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> classMaskCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<UUID>> prerequisitesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<UUID>> passedSubjectsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, com.example.datn.Model.Student> studentByUsernameCache = new ConcurrentHashMap<>();

    // --- Write Methods (Dùng lúc Warmup) ---
    public void putClassMetadata(UUID id, ClassSectionCacheDTO dto) { classMetadataCache.put(id, dto); }
    public void putClassMask(UUID id, String mask) { classMaskCache.put(id, mask); }
    public void putPrerequisites(UUID subjectId, Set<UUID> prereqIds) { prerequisitesCache.put(subjectId, prereqIds); }
    public void putPassedSubjects(UUID studentId, Set<UUID> passedIds) { passedSubjectsCache.put(studentId, passedIds); }

    // --- Read Methods (Dùng lúc Đăng ký tín chỉ - KHÔNG BLOCK) ---
    public ClassSectionCacheDTO getClassMetadata(UUID id) { return classMetadataCache.get(id); }
    public String getClassMask(UUID id) { return classMaskCache.get(id); }
    
    public Set<UUID> getPrerequisites(UUID subjectId) { 
        return prerequisitesCache.getOrDefault(subjectId, Collections.emptySet()); 
    }
    
    public Set<UUID> getPassedSubjects(UUID studentId) { 
        return passedSubjectsCache.getOrDefault(studentId, Collections.emptySet()); 
    }
    
    public com.example.datn.Model.Student getStudentByUsername(String username) {
        return studentByUsernameCache.get(username);
    }
    
    public void putStudentByUsername(String username, com.example.datn.Model.Student student) {
        studentByUsernameCache.put(username, student);
    }

    public void logStats() {
        log.info("Local Cache Stats - Metadata: {}, Masks: {}, Prerequisites: {}, PassedSubjects: {}", 
            classMetadataCache.size(), classMaskCache.size(), prerequisitesCache.size(), passedSubjectsCache.size());
    }
}
