# Tài liệu Đặc tả Cơ sở dữ liệu - Dự án DATN

Tài liệu này mô tả chi tiết cấu trúc các bảng trong cơ sở dữ liệu của dự án.

---

### 1. Bảng `academic_years` (Năm học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh duy nhất cho năm học. |
| 2 | name | VARCHAR(255) | Tên năm học (VD: 2023-2024). |
| 3 | is_current | BOOLEAN | Đánh dấu nếu là năm học hiện tại. |

---

### 2. Bảng `admin_classes` (Lớp hành chính)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh duy nhất cho lớp hành chính. |
| 2 | name | VARCHAR(255) | Tên lớp hành chính. |
| 3 | major_id | UUID, FK | Liên kết tới bảng majors. |
| 4 | cohort_id | UUID, FK | Liên kết tới bảng cohorts. |

---

### 3. Bảng `ai_recommendation_history` (Lịch sử gợi ý AI)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh duy nhất cho bản ghi gợi ý. |
| 2 | student_id | UUID, FK | Sinh viên nhận gợi ý. |
| 3 | semester_id | UUID, FK | Học kỳ áp dụng gợi ý. |
| 4 | input_hash | VARCHAR(64) | Hash của dữ liệu đầu vào để kiểm tra trùng lặp. |
| 5 | recommendation_json | TEXT | Nội dung gợi ý định dạng JSON. |
| 6 | raw_ai_response | TEXT | Phản hồi thô từ mô hình AI. |
| 7 | status | VARCHAR(50) | Trạng thái của gợi ý (ACTIVE, INACTIVE). |
| 8 | ai_model | VARCHAR(50) | Tên mô hình AI đã sử dụng. |
| 9 | created_at | TIMESTAMP | Thời gian tạo gợi ý. |

---

### 4. Bảng `class_sections` (Lớp học phần)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh lớp học phần. |
| 2 | section_code | VARCHAR(255) | Mã lớp học phần. |
| 3 | course_group_code | VARCHAR(20) | Mã nhóm học phần. |
| 4 | subject_id | UUID, FK | Môn học tương ứng. |
| 5 | subject_component_id | UUID, FK | Thành phần môn học (Lý thuyết/Thực hành). |
| 6 | parent_section_id | UUID, FK | Lớp học phần cha (nếu có). |
| 7 | semester_id | UUID, FK | Học kỳ mở lớp. |
| 8 | capacity | INT | Sức chứa tối đa. |
| 9 | min_students | INT | Số sinh viên tối thiểu để mở lớp. |
| 10 | enrolled_count | INT | Số sinh viên đã đăng ký hiện tại. |
| 11 | status | VARCHAR(20) | Trạng thái lớp (PENDING, OPEN, CLOSED). |
| 12 | version | BIGINT | Dùng cho Optimistic Locking. |

---

### 5. Bảng `cohorts` (Khóa học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh khóa học. |
| 2 | name | VARCHAR(255) | Tên khóa học (VD: K18). |
| 3 | start_year | INT | Năm bắt đầu khóa học. |

---

### 6. Bảng `education_programs` (Chương trình đào tạo)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh chương trình đào tạo. |
| 2 | code | VARCHAR(50) | Mã chương trình đào tạo. |
| 3 | name | VARCHAR(100) | Tên chương trình. |
| 4 | total_credits | INT | Tổng số tín chỉ yêu cầu. |
| 5 | duration_years | FLOAT | Thời gian đào tạo chuẩn (năm). |
| 6 | is_template | BOOLEAN | Đánh dấu nếu là mẫu để clone. |
| 7 | major_id | UUID, FK | Ngành đào tạo tương ứng. |
| 8 | is_active | BOOLEAN | Trạng thái hoạt động. |

---

### 7. Bảng `enrollments` (Đăng ký học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh bản ghi đăng ký. |
| 2 | student_id | UUID, FK | Sinh viên đăng ký. |
| 3 | class_section_id | UUID, FK | Lớp học phần đăng ký. |
| 4 | enrollment_date | TIMESTAMP | Ngày giờ đăng ký. |
| 5 | status | VARCHAR(50) | Trạng thái đăng ký (REGISTERED, DROPPED). |

---

### 8. Bảng `faculties` (Khoa)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh khoa. |
| 2 | name | VARCHAR(255) | Tên khoa. |
| 3 | code | VARCHAR(255) | Mã khoa. |
| 4 | established_at | TIMESTAMP | Ngày thành lập. |

---

### 9. Bảng `invoices` (Hóa đơn học phí)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh hóa đơn. |
| 2 | student_id | UUID, FK | Sinh viên nhận hóa đơn. |
| 3 | semester_id | UUID, FK | Học kỳ tính học phí. |
| 4 | total_amount | DECIMAL(19,2) | Tổng số tiền cần nộp. |
| 5 | paid_amount | DECIMAL(19,2) | Số tiền đã nộp. |
| 6 | status | VARCHAR(50) | Trạng thái thanh toán (PAID, UNPAID). |
| 7 | due_date | TIMESTAMP | Hạn chót thanh toán. |

---

### 10. Bảng `invoice_details` (Chi tiết hóa đơn)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã chi tiết hóa đơn. |
| 2 | invoice_id | UUID, FK | Thuộc hóa đơn nào. |
| 3 | subject_id | UUID, FK | Môn học tính phí. |
| 4 | credits | INT | Số tín chỉ môn học tại thời điểm tính. |
| 5 | unit_price | DECIMAL(19,2) | Đơn giá mỗi tín chỉ. |
| 6 | sub_total | DECIMAL(19,2) | Thành tiền môn học này. |

---

### 11. Bảng `lecturers` (Giảng viên)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh giảng viên. |
| 2 | lecturer_code | VARCHAR(255) | Mã số giảng viên. |
| 3 | user_id | UUID, FK | Tài khoản người dùng tương ứng. |
| 4 | full_name | VARCHAR(255) | Họ tên đầy đủ. |
| 5 | phone | VARCHAR(255) | Số điện thoại. |
| 6 | address | TEXT | Địa chỉ cư trú. |
| 7 | degree | VARCHAR(255) | Học vị/Bằng cấp. |
| 8 | gender | VARCHAR(50) | Giới tính. |
| 9 | status | VARCHAR(50) | Trạng thái công tác. |
| 10 | faculty_id | UUID, FK | Khoa trực thuộc. |

---

### 12. Bảng `majors` (Ngành học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh ngành học. |
| 2 | name | VARCHAR(255) | Tên ngành học. |
| 3 | code | VARCHAR(255) | Mã ngành (VD: KTPM). |
| 4 | is_active | BOOLEAN | Trạng thái hoạt động. |

---

### 13. Bảng `payments` (Lịch sử thanh toán)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã giao dịch thanh toán. |
| 2 | invoice_id | UUID, FK | Hóa đơn được thanh toán. |
| 3 | amount | DECIMAL(19,2) | Số tiền thanh toán lần này. |
| 4 | transaction_code | VARCHAR(255) | Mã giao dịch ngân hàng/cổng thanh toán. |
| 5 | payment_method | VARCHAR(50) | Phương thức (Chuyển khoản, Tiền mặt...). |
| 6 | payment_date | TIMESTAMP | Ngày thực hiện thanh toán. |

---

### 14. Bảng `period_cohorts` (Mở đăng ký theo khóa)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh. |
| 2 | period_id | UUID, FK | Đợt đăng ký tương ứng. |
| 3 | cohort_id | UUID, FK | Khóa được phép đăng ký. |
| 4 | start_time | TIMESTAMP | Thời gian bắt đầu riêng cho khóa này. |
| 5 | end_time | TIMESTAMP | Thời gian kết thúc riêng cho khóa này. |

---

### 15. Bảng `prerequisites` (Môn học tiên quyết)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh quan hệ. |
| 2 | subject_id | UUID, FK | Môn học hiện tại. |
| 3 | prerequisite_id | UUID, FK | Môn học tiên quyết/điều kiện. |

---

### 16. Bảng `program_cohorts` (Phân bổ CTĐT cho khóa)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh. |
| 2 | program_id | UUID, FK | Chương trình đào tạo. |
| 3 | cohort_id | UUID, FK | Áp dụng cho khóa nào. |
| 4 | applied_date | DATE | Ngày áp dụng. |

---

### 17. Bảng `program_subjects` (Môn học trong chương trình)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh bản ghi. |
| 2 | section_id | UUID, FK | Nhóm môn học (SubjectGroupSection). |
| 3 | subject_id | UUID, FK | Môn học trong danh mục. |
| 4 | semester | INT | Học kỳ gợi ý (1, 2, ...). |
| 5 | weight | DOUBLE | Trọng số môn học. |

---

### 18. Bảng `registration_periods` (Đợt đăng ký môn học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh đợt đăng ký. |
| 2 | semester_id | UUID, FK | Thuộc học kỳ nào. |
| 3 | name | VARCHAR(255) | Tên đợt đăng ký. |
| 4 | start_time | TIMESTAMP | Thời gian bắt đầu đợt. |
| 5 | end_time | TIMESTAMP | Thời gian kết thúc đợt. |
| 6 | is_active | BOOLEAN | Trạng thái đợt. |

---

### 19. Bảng `roles` (Vai trò)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh vai trò. |
| 2 | name | VARCHAR(255) | Tên vai trò (ROLE_STUDENT, ROLE_ADMIN...). |
| 3 | description | TEXT | Mô tả quyền hạn của vai trò. |

---

### 20. Bảng `rooms` (Phòng học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh phòng. |
| 2 | name | VARCHAR(255) | Tên phòng (VD: G3-101). |
| 3 | room_type_id | UUID, FK | Loại phòng (Lý thuyết, Lab...). |
| 4 | capacity | INT | Sức chứa của phòng. |

---

### 21. Bảng `room_types` (Loại phòng)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh loại phòng. |
| 2 | code | VARCHAR(50) | Mã loại phòng. |
| 3 | name | VARCHAR(100) | Tên loại phòng. |

---

### 22. Bảng `schedules` (Lịch học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh lịch học. |
| 2 | class_section_id | UUID, FK | Lớp học phần tương ứng. |
| 3 | room_id | UUID, FK | Phòng học được sắp xếp. |
| 4 | lecturer_id | UUID, FK | Giảng viên đứng lớp. |
| 5 | day_of_week | INT | Thứ trong tuần (2-8). |
| 6 | start_period | INT | Tiết bắt đầu. |
| 7 | end_period | INT | Tiết kết thúc. |
| 8 | is_deleted | BOOLEAN | Đánh dấu xóa mềm. |

---

### 23. Bảng `section_defaults` (Nhóm môn học mặc định)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh. |
| 2 | title | VARCHAR(255) | Tiêu đề nhóm môn. |
| 3 | subject_group_id | UUID, FK | Khối kiến thức cha. |
| 4 | is_mandatory | BOOLEAN | Bắt buộc hay tự chọn. |

---

### 24. Bảng `section_default_subjects` (Môn học trong nhóm mặc định)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh. |
| 2 | section_default_id | UUID, FK | Nhóm môn học mặc định. |
| 3 | subject_id | UUID, FK | Môn học cụ thể. |
| 4 | default_semester | INT | Học kỳ mặc định. |

---

### 25. Bảng `semesters` (Học kỳ)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh học kỳ. |
| 2 | name | VARCHAR(255) | Tên học kỳ (VD: Học kỳ 1). |
| 3 | academic_year_id | UUID, FK | Thuộc năm học nào. |
| 4 | start_date | DATE | Ngày bắt đầu học kỳ. |
| 5 | end_date | DATE | Ngày kết thúc học kỳ. |
| 6 | is_current | BOOLEAN | Đánh dấu học kỳ hiện tại. |

---

### 26. Bảng `students` (Sinh viên)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh sinh viên. |
| 2 | student_code | VARCHAR(255) | Mã số sinh viên. |
| 3 | user_id | UUID, FK | Tài khoản đăng nhập tương ứng. |
| 4 | full_name | VARCHAR(255) | Họ tên đầy đủ sinh viên. |
| 5 | cohort_id | UUID, FK | Thuộc khóa học nào. |
| 6 | major_id | UUID, FK | Thuộc ngành học nào. |
| 7 | admin_class_id | UUID, FK | Thuộc lớp hành chính nào. |
| 8 | status | VARCHAR(50) | Trạng thái (Đang học, Bảo lưu...). |

---

### 27. Bảng `student_grades` (Điểm số sinh viên)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh điểm số. |
| 2 | enrollment_id | UUID, FK | Liên kết tới lần đăng ký học. |
| 3 | midterm_score | DOUBLE | Điểm giữa kỳ. |
| 4 | final_score | DOUBLE | Điểm cuối kỳ. |
| 5 | total_score | DOUBLE | Điểm tổng kết hệ 10. |
| 6 | is_passed | BOOLEAN | Trạng thái qua môn. |

---

### 28. Bảng `subjects` (Môn học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh môn học. |
| 2 | code | VARCHAR(255) | Mã môn học. |
| 3 | name | VARCHAR(255) | Tên môn học. |
| 4 | credits | INT | Số tín chỉ. |
| 5 | total_periods | INT | Tổng số tiết học. |
| 6 | coffee | DOUBLE | Hệ số học phí môn học. |

---

### 29. Bảng `subject_components` (Thành phần môn học)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh thành phần. |
| 2 | subject_id | UUID, FK | Thuộc môn học nào. |
| 3 | type | VARCHAR(50) | Loại (Lý thuyết, Thực hành). |
| 4 | sessions_per_week | INT | Số buổi mỗi tuần. |
| 5 | periods_per_session | INT | Số tiết mỗi buổi. |

---

### 30. Bảng `subject_groups` (Khối kiến thức)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh khối kiến thức. |
| 2 | name | VARCHAR(255) | Tên khối (VD: Kiến thức chuyên ngành). |
| 3 | index | INT | Thứ tự hiển thị. |
| 4 | is_global | BOOLEAN | Có dùng chung cho mọi ngành không. |

---

### 31. Bảng `subject_group_sections` (Nhóm học phần trong khối)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh. |
| 2 | title | VARCHAR(500) | Tiêu đề nhóm. |
| 3 | education_program_id | UUID, FK | Thuộc CTĐT nào. |
| 4 | subject_group_id | UUID, FK | Thuộc khối kiến thức nào. |
| 5 | required_credits | INT | Số tín chỉ tối thiểu của nhóm. |

---

### 32. Bảng `users` (Người dùng hệ thống)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh người dùng. |
| 2 | username | VARCHAR(255) | Tên đăng nhập. |
| 3 | email | VARCHAR(255) | Địa chỉ email. |
| 4 | password | VARCHAR(255) | Mật khẩu băm. |
| 5 | is_active | BOOLEAN | Tài khoản còn hoạt động không. |
| 6 | avatar_url | VARCHAR(255) | Đường dẫn ảnh đại diện. |

---

### 33. Bảng `user_roles` (Phân quyền người dùng)
| STT | Tên Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | id | UUID, PK | Mã định danh phân quyền. |
| 2 | user_id | UUID, FK | Người dùng. |
| 3 | role_id | UUID, FK | Vai trò được gán. |
| 4 | granted_at | TIMESTAMP | Ngày cấp quyền. |
