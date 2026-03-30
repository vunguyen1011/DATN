package com.example.datn.ENUM;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String vnName;

    // Hàm "thần thánh" để chuyển đổi từ String sang Enum
    public static Gender fromVnString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER; // Mặc định là Khác nếu để trống
        }

        for (Gender gender : Gender.values()) {
            if (gender.vnName.equalsIgnoreCase(value.trim())) {
                return gender;
            }
        }
        return OTHER; // Không khớp thì trả về Khác thay vì ném lỗi
    }
}