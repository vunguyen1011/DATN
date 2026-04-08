package com.example.datn.Exception;

public enum ErrorCode {

    UNAUTHENTICATED(401, "Unauthenticated"),

    // 10xx - 1099: Auth & User
    USER_NOT_FOUND(1001, "User not found"),
    USER_ALREADY_EXISTS(1002, "User already exists"),
    ROLE_NOT_FOUND(1003, "Role not found"),
    USER_NOT_ACTIVE(1004, "User not active"),
    LOGIN_FAILED(1005,"Username or password is incorrect"),
    INVALID_CREDENTIALS(1006, "Sai username hoặc password"),
    USER_DISABLED(1007, "Tài khoản chưa được kích hoạt"),
    USER_LOCKED(1008, "Tài khoản bị khóa"),
    METHOD_NOT_SUPPORTED(1009, "Phương thức xác thực không được hỗ trợ"),
    PASSWORD_NOT_MATCH(1010, "Mật khẩu không khớp"),
    INVALID_OTP(1011, "Mã OTP không hợp lệ"),
    INVALID_TOKEN(1112, "Token không hợp lệ hoặc đã hết hạn"),
    USER_ALREADY_HAS_ROLE(1013, "User đã có role này"),

    // 11xx: Major
    MAJOR_CODE_EXISTED(1101, "Mã ngành đã tồn tại"),
    MAJOR_NAME_EXISTED(1102, "Tên ngành đã tồn tại"),
    MAJOR_NOT_FOUND(1103, "Ngành học không tồn tại"),

    // 12xx: Cohort
    COHORT_NAME_EXISTED(1201, "Tên khóa học đã tồn tại"),
    COHORT_NOT_FOUND(1202, "Khóa học không tồn tại"),

    // 13xx: Academic Year
    ACADEMIC_YEAR_NAME_EXISTED(1301, "Tên năm học đã tồn tại"),
    ACADEMIC_YEAR_NOT_FOUND(1302, "Năm học không tồn tại"),

    // 14xx: Admin Class
    ADMIN_CLASS_NAME_EXISTED(1401, "Tên lớp học đã tồn tại"),
    ADMIN_CLASS_NOT_FOUND(1402, "Lớp học không tồn tại"),
    ADMIN_CLASS_CODE_EXISTED(1403, "Mã lớp học đã tồn tại"),

    // 15xx: Semester
    SEMESTER_NAME_EXISTED_IN_YEAR(1501, "Tên học kỳ đã tồn tại trong năm học này"),
    SEMESTER_NOT_FOUND(1502, "Học kỳ không tồn tại"),
    INVALID_DATE_RANGE(1503, "Ngày bắt đầu phải trước ngày kết thúc"),

    // 16xx: Subject & Subject Group
    SUBJECT_EXISTED(1601, "Môn học đã tồn tại"),
    SUBJECT_NOT_FOUND(1602, "Môn học không tồn tại"),
    SUBJECT_INVALID_PREREQUISITE(1603, "Môn học không thể làm điều kiện tiên quyết cho chính nó"),
    INVALID_CREDIT_RANGE(1604, "Số tín chỉ không hợp lệ"),
    SUBJECT_GROUP_NOT_FOUND(1605, "Nhóm môn học không tồn tại"),
    SUBJECT_GROUP_ALREADY_EXISTS(1606, "Tên nhóm môn học đã tồn tại"),
    SUBJECT_ALREADY_EXISTS_IN_SECTION(1607, "Môn học đã tồn tại trong nhóm lớp học"),
    CIRCULAR_PREREQUISITE_DETECTED(1608, "Phát hiện vòng lặp trong điều kiện tiên quyết của môn học"),
    SUBJECT_IS_PREREQUISITE_CANNOT_DELETE(1609, "Môn học đang là điều kiện tiên quyết cho môn khác, không thể xóa"),
    SECTION_DEFAULT_SUBJECT_NOT_FOUND(1610, "Môn học trong nhóm lớp học mặc định không tồn tại"),
    SUBJECT_IN_USE_IN_PROGRAM(1611, "Môn học đang được sử dụng trong chương trình đào tạo, không thể xóa"),
    SUBJECT_COMPONENT_NOT_FOUND(1612, "Thành phần môn học không tồn tại"),
    ROOM_TYPE_NOT_FOUND(1613, "Loại phòng không tồn tại"),
    // 17xx: Education Program & Sections
    SECTION_NOT_FOUND(1701, "Nhóm lớp học không tồn tại"),
    PROGRAM_SUBJECT_NOT_FOUND(1702, "Môn học trong chương trình đào tạo không tồn tại"),
    PROGRAM_ALREADY_EXISTS(1703, "Chương trình đào tạo đã tồn tại"),
    PROGRAM_CODE_ALREADY_EXISTS(1704, "Mã chương trình đào tạo đã tồn tại"),
    PROGRAM_SUBJECT_GROUP_NOT_FOUND(1705, "Nhóm môn học trong chương trình đào tạo không tồn tại"),
    SECTION_CONTAINS_SUBJECTS(1706, "Nhóm lớp học đang chứa môn học, không thể xóa"),
    REQUIRED_CREDITS_MISSING(1707, "Số tín không phù hợp cho nhóm môn học bắt buộc"),
    SUBJECT_IN_USE_IN_TEMPLATE(1708, "Môn học đang được sử dụng trong mẫu chương trình đào tạo, không thể xóa"),
    SECTION_DEFAULT_NOT_FOUND(1709, "Không tìm thấy nhóm lớp học mặc định cho chương trình đào tạo"),
    PROGRAM_NOT_FOUND(1710, "Chương trình đào tạo không tồn tại"),
    SECTION_DEFAULT_TITLE_ALREADY_EXISTS(1711, "Tiêu đề nhóm lớp học mặc định đã tồn tại trong chương trình đào tạo này"),
    PROGRAM_COHORT_ALREADY_EXISTS(1712, "Chương trình đào tạo đã được gắn với khóa học này"),
    PROGRAM_COHORT_NOT_FOUND(1713, "Liên kết chương trình đào tạo - khóa học không tồn tại"),
    PROGRAM_HAS_STUDENTS_CANNOT_DELETE(1714, "Chương trình đào tạo đang có sinh viên theo học, không thể xóa"),
    PROGRAM_IS_LOCKED(1715, "Chương trình đào tạo đã được khóa (published), không thể thêm/sửa/xóa môn học"),
    PROGRAM_ALREADY_PUBLISHED(1716, "Chương trình đào tạo đã được publish trước đó"),
    PROGRAM_NOT_PUBLISHED_CANNOT_ASSIGN(1718, "Chương trình đào tạo chưa được publish, không thể gán cho khóa học"),
    PROGRAM_HAS_NO_COHORTS_CANNOT_PUBLISH(1717, "Chương trình đào tạo chưa có khóa học nào, không thể publish"),
    // 19xx: System Errors
    UNCATEGORIZED_EXCEPTION(1901, "Lỗi không xác định"),

    // 20xx: Excel & Import
    INVALID_EXCEL_FORMAT(2001, "Chỉ chấp nhận file Excel định dạng .xlsx"),
    EXCEL_HEADER_MISMATCH(2002, "Cấu trúc file không khớp với file mẫu. Vui lòng tải lại file"),
    EXCEL_FILE_EMPTY(2003, "File Excel không có dữ liệu sinh viên"),
    EXCEL_READ_ERROR(2004, "Lỗi hệ thống khi đọc dữ liệu từ file Excel"),
    INVALID_FILE_FORMAT(2005, "Định dạng file không hợp lệ. Vui lòng tải lên file Excel (.xlsx)"),
    EXCEL_DATA_INVALID(2006, "Dữ liệu trong file Excel không hợp lệ hoặc thiếu thông tin bắt buộc"),
    EXCEL_IMPORT_PARTIAL_SUCCESS(2007, "Import thành công một phần, một số dòng bị lỗi hoặc trùng lặp");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}