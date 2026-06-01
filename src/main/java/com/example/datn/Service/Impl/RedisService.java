package com.example.datn.Service.Impl;

import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ClassSectionRepository classSectionRepository;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String OTP_PREFIX = "OTP:";
    private static final String RESET_TOKEN_PREFIX = "RESET_TOKEN:";

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
        java.util.List<com.example.datn.Model.ClassSection> sections = classSectionRepository
                .findBySemesterId(semesterId);

        int count = 0;
        for (com.example.datn.Model.ClassSection section : sections) {
            String slotKey = "class_slot:" + section.getId();
            String setKey = "class_students:" + section.getId();
            String subjectKey = "student_subject:" + section.getSubject().getId();

            // Số slot còn lại = capacity - enrolledCount (đảm bảo không âm)
            int available = Math.max(0, section.getCapacity() - section.getEnrolledCount());

            redisTemplate.opsForValue().set(slotKey, String.valueOf(available));
            // Xoá Set sinh viên cũ (reset sạch để tránh dữ liệu thừa từ đợt trước)
            redisTemplate.delete(setKey);
            redisTemplate.delete(subjectKey);
            count++;
        }
        log.info("[SYNC] Đã đồng bộ {} lớp học phần của học kỳ {} lên Redis.", count, semesterId);
    }

    @Override
    public int tryAcquireSlot(java.util.UUID theoryId, java.util.UUID labId, java.util.UUID studentId, java.util.UUID subjectId, String theoryMask, String labMask) {
        String theorySlotKey = "class_slot:" + theoryId;
        String theorySetKey = "class_students:" + theoryId;
        
        String labSlotKey = labId != null ? "class_slot:" + labId : "dummy_lab_slot";
        String labSetKey = labId != null ? "class_students:" + labId : "dummy_lab_set";
        
        String subjectKey = subjectId != null ? "student_subject:" + subjectId : "dummy_subject_key";
        String studentMaskKey = "student_mask:" + studentId;

        String luaScript = 
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

        DefaultRedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        String hasLabStr = labId != null ? "true" : "false";
        java.util.List<String> keys = java.util.Arrays.asList(theorySlotKey, theorySetKey, labSlotKey, labSetKey, subjectKey, studentMaskKey);
        
        Long result = redisTemplate.execute(script, keys, studentId.toString(), theoryMask != null ? theoryMask : "", labMask != null ? labMask : "", hasLabStr);
        return result != null ? result.intValue() : -1;
    }

    @Override
    public void releaseSlot(java.util.UUID classSectionId, java.util.UUID studentId, java.util.UUID subjectId) {
        String slotKey = "class_slot:" + classSectionId;
        String setKey = "class_students:" + classSectionId;

        Long removed = redisTemplate.opsForSet().remove(setKey, studentId.toString());
        if (removed != null && removed > 0) {
            redisTemplate.opsForValue().increment(slotKey);
        }
        
        if (subjectId != null) {
            String subjectKey = "student_subject:" + subjectId;
            redisTemplate.opsForSet().remove(subjectKey, studentId.toString());
        }
    }

    @Override
    public void clearRegistrationData() {
        String slotKeyPattern = "class_slot:*";
        String setKeyPattern = "class_students:*";
        String subjectKeyPattern = "student_subject:*";

        java.util.Set<String> slotKeys = redisTemplate.keys(slotKeyPattern);
        java.util.Set<String> setKeys = redisTemplate.keys(setKeyPattern);
        java.util.Set<String> subjectKeys = redisTemplate.keys(subjectKeyPattern);

        if (slotKeys != null && !slotKeys.isEmpty()) {
            redisTemplate.delete(slotKeys);
            log.info("Đã xóa {} key class_slot", slotKeys.size());
        }

        if (setKeys != null && !setKeys.isEmpty()) {
            redisTemplate.delete(setKeys);
            log.info("Đã xóa {} key class_students", setKeys.size());
        }

        if (subjectKeys != null && !subjectKeys.isEmpty()) {
            redisTemplate.delete(subjectKeys);
            log.info("Đã xóa {} key student_subject", subjectKeys.size());
        }
    }

    @Override
    public long incrementAndCheckRateLimit(String key, int windowInSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowInSeconds));
        }
        return count != null ? count : 0;
    }
}