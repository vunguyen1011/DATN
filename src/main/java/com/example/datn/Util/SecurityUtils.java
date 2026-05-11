package com.example.datn.Util;

import com.example.datn.Security.MyUserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public static UUID getCurrentStudentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MyUserDetail userDetails) {
            return userDetails.getStudentId();
        }
        return null;
    }

    // Lưu ý: Semester hiện tại thường được lưu trong DB hoặc Config. 
    // Ở đây tôi cung cấp một placeholder, bạn có thể thay đổi cách lấy cho phù hợp.
    // Nếu RegistrationServiceImpl đã có semesterRepository, ta sẽ lấy từ đó.
}
