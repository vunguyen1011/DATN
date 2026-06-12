package com.example.datn.Service.Impl;

import com.example.datn.Model.ClassSection;
import com.example.datn.Model.Schedule;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String OTP_PREFIX = "OTP:";
    private static final String RESET_TOKEN_PREFIX = "RESET_TOKEN:";
    private static final String LUA_SCRIPT_TEXT =
            "local theorySlotKey = KEYS[1]\n" +
                    "local theorySetKey = KEYS[2]\n" +
                    "local labSlotKey = KEYS[3]\n" +
                    "local labSetKey = KEYS[4]\n" +
                    "local subjectKey = KEYS[5]\n" +
                    "local studentMaskKey = KEYS[6]\n" +
                    "local studentId = ARGV[1]\n" +
                    "local theoryMaskJson = ARGV[2]\n" +
                    "local labMaskJson = ARGV[3]\n" +
                    "local hasLab = ARGV[4]\n" +
                    // 1. Check if already enrolled in theory
                    "if redis.call('SISMEMBER', theorySetKey, studentId) == 1 then\n" +
                    "    return 0\n" +
                    "end\n" +
                    // 2. Check if already enrolled in another section of this subject
                    "if redis.call('SISMEMBER', subjectKey, studentId) == 1 then\n" +
                    "    return -2\n" +
                    "end\n" +
                    // 3. Check Theory Slot
                    "local theoryCurrent = redis.call('GET', theorySlotKey)\n" +
                    "if not theoryCurrent or tonumber(theoryCurrent) <= 0 then\n" +
                    "    return -1\n" +
                    "end\n" +
                    // 4. Check Lab Slot
                    "if hasLab == 'true' then\n" +
                    "    if redis.call('SISMEMBER', labSetKey, studentId) == 1 then\n" +
                    "        return 0\n" +
                    "    end\n" +
                    "    local labCurrent = redis.call('GET', labSlotKey)\n" +
                    "    if not labCurrent or tonumber(labCurrent) <= 0 then\n" +
                    "        return -4\n" +
                    "    end\n" +
                    "end\n" +
                    // 5. Check Mask Overlap
                    "local studentMaskStr = redis.call('GET', studentMaskKey)\n" +
                    "local studentMask = {}\n" +
                    "if studentMaskStr and studentMaskStr ~= '' then\n" +
                    "    studentMask = cjson.decode(studentMaskStr)\n" +
                    "end\n" +
                    "local tMask = {}\n" +
                    "if theoryMaskJson and theoryMaskJson ~= '' then\n" +
                    "    tMask = cjson.decode(theoryMaskJson)\n" +
                    "end\n" +
                    "local lMask = {}\n" +
                    "if hasLab == 'true' and labMaskJson and labMaskJson ~= '' then\n" +
                    "    lMask = cjson.decode(labMaskJson)\n" +
                    "end\n" +
                    "for i=1, 9 do\n" +
                    "    local s = studentMask[i] or 0\n" +
                    "    local t = tMask[i] or 0\n" +
                    "    local l = lMask[i] or 0\n" +
                    "    if bit.band(s, t) ~= 0 or bit.band(s, l) ~= 0 or bit.band(t, l) ~= 0 then\n" +
                    "        return -3\n" +
                    "    end\n" +
                    "end\n" +
                    // 6. ALL CHECKS PASSED -> COMMIT
                    "redis.call('DECR', theorySlotKey)\n" +
                    "redis.call('SADD', theorySetKey, studentId)\n" +
                    "redis.call('SADD', subjectKey, studentId)\n" +
                    "if hasLab == 'true' then\n" +
                    "    redis.call('DECR', labSlotKey)\n" +
                    "    redis.call('SADD', labSetKey, studentId)\n" +
                    "end\n" +
                    "for i=1, 9 do\n" +
                    "    local s = studentMask[i] or 0\n" +
                    "    local t = tMask[i] or 0\n" +
                    "    local l = lMask[i] or 0\n" +
                    "    studentMask[i] = bit.bor(s, bit.bor(t, l))\n" +
                    "end\n" +
                    "redis.call('SET', studentMaskKey, cjson.encode(studentMask))\n" +
                    "return 1";
    private static final String RELEASE_SLOT_LUA =
            "local slotKey = KEYS[1]\n" +
                    "local setKey = KEYS[2]\n" +
                    "local subjectKey = KEYS[3]\n" +
                    "local studentMaskKey = KEYS[4]\n" +
                    "local studentId = ARGV[1]\n" +
                    "local classMaskJson = ARGV[2]\n" +
                    "local removed = redis.call('SREM', setKey, studentId)\n" +
                    "if removed == 1 then\n" +
                    "    redis.call('INCR', slotKey)\n" +
                    "    if subjectKey ~= 'dummy_subject_key' then\n" +
                    "        redis.call('SREM', subjectKey, studentId)\n" +
                    "    end\n" +
                    "    if studentMaskKey ~= 'dummy_mask_key' and classMaskJson and classMaskJson ~= '' then\n" +
                    "        local studentMaskStr = redis.call('GET', studentMaskKey)\n" +
                    "        if studentMaskStr and studentMaskStr ~= '' then\n" +
                    "            local studentMask = cjson.decode(studentMaskStr)\n" +
                    "            local classMask = cjson.decode(classMaskJson)\n" +
                    "            for i=1, 9 do\n" +
                    "                local s = studentMask[i] or 0\n" +
                    "                local c = classMask[i] or 0\n" +
                    "                studentMask[i] = bit.band(s, bit.bnot(c))\n" +
                    "            end\n" +
                    "            redis.call('SET', studentMaskKey, cjson.encode(studentMask))\n" +
                    "        end\n" +
                    "    end\n" +
                    "end\n" +
                    "return removed";
    private static final String RATE_LIMIT_LUA =
            "local c = redis.call('INCR', KEYS[1])\n" +
                    "if c == 1 then\n" +
                    "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
                    "end\n" +
                    "return c";
    private static final DefaultRedisScript<Long> ACQUIRE_SLOT_SCRIPT;
    private static final DefaultRedisScript<Long> RELEASE_SLOT_SCRIPT;
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        ACQUIRE_SLOT_SCRIPT = new DefaultRedisScript<>();
        ACQUIRE_SLOT_SCRIPT.setScriptText(LUA_SCRIPT_TEXT);
        ACQUIRE_SLOT_SCRIPT.setResultType(Long.class);
        RELEASE_SLOT_SCRIPT = new DefaultRedisScript<>();
        RELEASE_SLOT_SCRIPT.setScriptText(RELEASE_SLOT_LUA);
        RELEASE_SLOT_SCRIPT.setResultType(Long.class);
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(RATE_LIMIT_LUA);
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final ClassSectionRepository classSectionRepository;
    private final com.example.datn.Repository.ScheduleRepository scheduleRepository;
    private final com.example.datn.Repository.EnrollmentRepository enrollmentRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void saveRefreshToken(String username, String refreshToken, Duration duration) {
        String key = buildKey(username);
        try {
            redisTemplate.opsForValue().set(key, refreshToken, duration);
            log.info("Đã lưu Refresh Token vào Redis cho user: {}", username);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Refresh Token vào Redis cho user: {}", username, e);
        }
    }

    @Override
    public String getRefreshToken(String username) {
        String key = buildKey(username);
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy Refresh Token từ Redis cho user: {}", username, e);
            return null;
        }
    }

    @Override
    public void deleteRefreshToken(String username) {
        String key = buildKey(username);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa Refresh Token trong Redis của user: {}", username);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa Refresh Token trong Redis của user: {}", username, e);
        }
    }

    @Override
    public boolean isValidRefreshToken(String username, String refreshToken) {
        String storedToken = getRefreshToken(username);
        if (storedToken == null) {
            log.warn("Không tìm thấy Refresh Token trong Redis cho user: {}", username);
            return false;
        }
        if (!storedToken.equals(refreshToken)) {
            log.warn("Refresh Token gửi lên không khớp với Redis cho user: {}", username);
            return false;
        }
        return true;
    }

    @Override
    public void saveOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
            log.info("Đã lưu OTP vào Redis cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu OTP vào Redis cho email: {}", email, e);
        }
    }

    @Override
    public String getOtp(String email) {
        String key = OTP_PREFIX + email;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy OTP từ Redis cho email: {}", email, e);
            return null;
        }
    }

    @Override
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa OTP trong Redis của email: {}", email);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa OTP trong Redis của email: {}", email, e);
        }
    }

    @Override
    public void saveResetToken(String email, String resetToken, Duration duration) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, resetToken, duration);
            log.info("Đã lưu Reset Token vào Redis cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Reset Token vào Redis cho email: {}", email, e);
        }
    }

    @Override
    public String getResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy Reset Token từ Redis cho email: {}", email, e);
            return null;
        }
    }

    @Override
    public void deleteResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa Reset Token trong Redis của email: {}", email);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa Reset Token trong Redis của email: {}", email, e);
        }
    }

    private String buildKey(String username) {
        return REFRESH_TOKEN_PREFIX + username;
    }

    @Override
    public void saveRecommendation(String studentId, String jsonResponse, Duration duration) {
        String key = "AI_REC:" + studentId;
        try {
            redisTemplate.opsForValue().set(key, jsonResponse, duration);
            log.info("Đã lưu Recommendation vào Redis cho student: {}", studentId);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Recommendation vào Redis", e);
        }
    }

    @Override
    public String getRecommendation(String studentId) {
        String key = "AI_REC:" + studentId;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy Recommendation từ Redis", e);
            return null;
        }
    }

    @Override
    public void syncClassCapacityToRedis(java.util.UUID semesterId) {
        // Lấy toàn bộ lớp học phần trong học kỳ từ DB
        List<ClassSection> sections = classSectionRepository
                .findBySemesterId(semesterId);
       Set<String> deletedSubjects = new HashSet<>();
        int count = 0;
        for (ClassSection section : sections) {
            String slotKey = "class_slot:" + section.getId();
            String setKey = "class_students:" + section.getId();
            String subjectKey = "student_subject:" + section.getSubject().getId();
            String classMaskKey = "class_mask:" + section.getId();
            // Số slot còn lại = capacity - enrolledCount (đảm bảo không âm)
            int available = Math.max(0, section.getCapacity() - section.getEnrolledCount());
            redisTemplate.opsForValue().set(slotKey, String.valueOf(available));
            // Tính toán và lưu class_mask
            java.util.List<com.example.datn.Model.Schedule> schedules = scheduleRepository.findByClassSection_Id(section.getId());
            int[] mask = buildScheduleMask(schedules);
            try {
                redisTemplate.opsForValue().set(classMaskKey, objectMapper.writeValueAsString(mask));
            } catch (Exception e) {
                log.error("Lỗi parse mask cho lớp {}", section.getId(), e);
            }
            // Xoá Set sinh viên cũ (reset sạch để tránh dữ liệu thừa từ đợt trước)
            redisTemplate.delete(setKey);
            // Xóa subject key nhưng có kiểm tra tránh vòng lặp xóa nhiều lần cho cùng 1 môn
            if (!deletedSubjects.contains(subjectKey)) {
                redisTemplate.delete(subjectKey);
                deletedSubjects.add(subjectKey);
            }
            
            // Lấy danh sách sinh viên đã đăng ký lớp này từ DB và đưa vào Redis Set
            java.util.List<com.example.datn.Model.Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndStatus(
                    section.getId(), com.example.datn.ENUM.EnrollmentStatus.REGISTERED, org.springframework.data.domain.Pageable.unpaged()).getContent();
            if (!enrollments.isEmpty()) {
                String[] studentIds = enrollments.stream()
                        .map(e -> e.getStudent().getId().toString())
                        .toArray(String[]::new);
                redisTemplate.opsForSet().add(setKey, studentIds);
                redisTemplate.opsForSet().add(subjectKey, studentIds);
            }
            count++;
        }
        log.info("[SYNC] Đã đồng bộ {} lớp học phần của học kỳ {} lên Redis.", count, semesterId);
    }

    private int[] buildScheduleMask(java.util.List<com.example.datn.Model.Schedule> schedules) {
        int[] mask = new int[9];
        for (Schedule s : schedules) {
            if (s.getDayOfWeek() == null || s.getStartPeriod() == null || s.getEndPeriod() == null) continue;
            int day = s.getDayOfWeek();
            int start = s.getStartPeriod();
            int end = s.getEndPeriod();
            int periodMask = ((1 << (end - start + 1)) - 1) << start;
            mask[day] |= periodMask;
        }
        return mask;
    }

    @Override
    public String recalculateAndCacheClassMask(java.util.UUID classSectionId) {
        String classMaskKey = "class_mask:" + classSectionId;
        java.util.List<com.example.datn.Model.Schedule> schedules = scheduleRepository.findByClassSection_Id(classSectionId);
        int[] mask = buildScheduleMask(schedules);
        try {
            String maskStr = objectMapper.writeValueAsString(mask);
            redisTemplate.opsForValue().set(classMaskKey, maskStr);
            log.info("Đã load lại class_mask từ DB cho lớp {}", classSectionId);
            return maskStr;
        } catch (Exception e) {
            log.error("Lỗi parse mask fallback cho lớp {}", classSectionId, e);
            return null;
        }
    }

    @Override
    public int tryAcquireSlot(java.util.UUID theoryId, java.util.UUID labId, java.util.UUID studentId, java.util.UUID subjectId, String theoryMask, String labMask) {
        String theorySlotKey = "class_slot:" + theoryId;
        String theorySetKey = "class_students:" + theoryId;
        String labSlotKey = labId != null ? "class_slot:" + labId : "dummy_lab_slot";
        String labSetKey = labId != null ? "class_students:" + labId : "dummy_lab_set";
        String subjectKey = subjectId != null ? "student_subject:" + subjectId : "dummy_subject_key";
        String studentMaskKey = "student_mask:" + studentId;
        String hasLabStr = labId != null ? "true" : "false";
        java.util.List<String> keys = java.util.Arrays.asList(theorySlotKey, theorySetKey, labSlotKey, labSetKey, subjectKey, studentMaskKey);
        Long result = redisTemplate.execute(ACQUIRE_SLOT_SCRIPT, keys, studentId.toString(), theoryMask != null ? theoryMask : "", labMask != null ? labMask : "", hasLabStr);
        return result != null ? result.intValue() : -1;
    }

    @Override
    public void releaseSlot(java.util.UUID classSectionId, java.util.UUID studentId, java.util.UUID subjectId, String classMask) {
        String slotKey = "class_slot:" + classSectionId;
        String setKey = "class_students:" + classSectionId;
        String subjectKey = subjectId != null ? "student_subject:" + subjectId : "dummy_subject_key";
        String studentMaskKey = "student_mask:" + studentId;
        java.util.List<String> keys = java.util.Arrays.asList(slotKey, setKey, subjectKey, studentMaskKey);
        redisTemplate.execute(RELEASE_SLOT_SCRIPT, keys, studentId.toString(), classMask != null ? classMask : "");
    }

    private void deleteKeysByPattern(String pattern) {
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
            } catch (Exception e) {
                log.error("Lỗi khi scan và xóa pattern {}", pattern, e);
            }
            return null;
        });
    }

    @Override
    public void clearRegistrationData() {
        log.info("Bắt đầu dọn dẹp dữ liệu đăng ký cũ bằng phương pháp SCAN an toàn...");
        deleteKeysByPattern("class_slot:*");
        deleteKeysByPattern("class_students:*");
        deleteKeysByPattern("student_subject:*");
        deleteKeysByPattern("student_mask:*");
        deleteKeysByPattern("class_mask:*");
        log.info("Dọn dẹp hoàn tất. Hệ thống sẵn sàng cho học kỳ mới.");
    }

    @Override
    public long incrementAndCheckRateLimit(String key, int windowInSeconds) {
        Long result = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                java.util.Collections.singletonList(key),
                String.valueOf(windowInSeconds)
        );
        return result != null ? result : 0;
    }

    @Override
    public void addPendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId) {
        String key = "pending_reg:" + studentId + ":" + classSectionId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofHours(24));
    }

    @Override
    public void removePendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId) {
        String key = "pending_reg:" + studentId + ":" + classSectionId;
        redisTemplate.delete(key);
    }

    @Override
    public void expirePendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId, Duration duration) {
        String key = "pending_reg:" + studentId + ":" + classSectionId;
        redisTemplate.expire(key, duration);
    }

    @Override
    public java.util.Set<java.util.UUID> getPendingRegistrations(java.util.UUID studentId) {
        Set<java.util.UUID> pendingClassIds = new HashSet<>();
        String pattern = "pending_reg:" + studentId + ":*";

        redisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(50).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    String classIdStr = key.substring(key.lastIndexOf(":") + 1);
                    pendingClassIds.add(java.util.UUID.fromString(classIdStr));
                }
            } catch (Exception e) {
                log.error("Lỗi scan Redis keys cho sinh viên: {}", studentId, e);
            }
            return null;
        });

        return pendingClassIds;
    }

    @Override
    public void addPendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId) {
        String key = "pending_cancel:" + studentId + ":" + classSectionId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofHours(24));
    }

    @Override
    public void removePendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId) {
        String key = "pending_cancel:" + studentId + ":" + classSectionId;
        redisTemplate.delete(key);
    }

    @Override
    public void expirePendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId, Duration duration) {
        String key = "pending_cancel:" + studentId + ":" + classSectionId;
        redisTemplate.expire(key, duration);
    }

    @Override
    public java.util.Set<java.util.UUID> getPendingCancellations(java.util.UUID studentId) {
        Set<java.util.UUID> pendingClassIds = new HashSet<>();
        String pattern = "pending_cancel:" + studentId + ":*";

        redisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(50).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    String classIdStr = key.substring(key.lastIndexOf(":") + 1);
                    pendingClassIds.add(java.util.UUID.fromString(classIdStr));
                }
            } catch (Exception e) {
                log.error("Lỗi scan Redis keys pending_cancel cho sinh viên: {}", studentId, e);
            }
            return null;
        });

        return pendingClassIds;
    }
}