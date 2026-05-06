# Kế Hoạch Tối Ưu Hóa Hiệu Năng Đăng Ký Tín Chỉ (High TPS)

Mục tiêu của kế hoạch này là tối đa hóa chỉ số TPS (Transactions Per Second) của tính năng đăng ký tín chỉ thông qua 4 phương pháp tối ưu lõi: Thu hẹp Transaction, Caching, Kiểm tra In-memory, và Redis Atomic Counter.

## User Review Required

> [!IMPORTANT]
> Phương pháp 4 (Redis Atomic Counter) yêu cầu bạn phải gọi API (hoặc chạy logic) để đồng bộ sĩ số (`capacity`, `enrolled_count`) từ Database lên Redis **trước khi** đợt đăng ký bắt đầu. Kế hoạch này sẽ cung cấp API đồng bộ đó. Bạn có đồng ý với việc phải bấm nút "Đồng bộ lên Redis" (hoặc tự động đồng bộ) trước mỗi đợt đăng ký không?

> [!WARNING]
> Việc thu hẹp Transaction (Phương pháp 1) sẽ loại bỏ `@Transactional` trên toàn bộ hàm `enroll()`. Khối lệnh ghi CSDL (Update, Insert) sẽ được bọc lại thủ công bằng `TransactionTemplate`. Cần đảm bảo rằng các lỗi logic bắn ra ngoài khối ghi sẽ được thiết kế sao cho slot trong Redis được trả lại chính xác (Rollback cơ chế Redis Counter).

## Các Thay Đổi Đề Xuất

### 1. Thu hẹp Transaction (Method 1)
*   **File:** `RegistrationServiceImpl.java`
*   **Thay đổi:** Bỏ annotation `@Transactional` ở đầu hàm `enroll()`. Khởi tạo và tiêm `TransactionTemplate` (của Spring) vào Service. Dùng object này để bao bọc riêng đoạn code thực thi Ghi (Locking và Save Enrollment). Các thao tác Đọc và Validate sẽ nằm hoàn toàn ngoài transaction, giúp giải phóng DB connection ngay lập tức.

### 2. Caching Dữ Liệu Tĩnh (Method 2)
*   Sử dụng **Caffeine Cache** (Local cache cực nhanh trên RAM) cho dữ liệu cấu hình ít thay đổi để có tốc độ đọc `< 1ms`.
*   **File:** `pom.xml` (thêm dependency `spring-boot-starter-cache` và `caffeine`).
*   **File:** `DatnApplication.java` hoặc `CacheConfig.java`: Thêm `@EnableCaching`.
*   **File:** `PeriodCohortRepository.java` & `PrerequisiteRepository.java`.
    *   Thêm `@Cacheable("periodCohort")` vào `findOngoingCohortPeriod`.
    *   Thêm `@Cacheable("prerequisites")` vào `findBySubjectId`.

### 3. Kiểm Tra Trùng Lịch In-Memory (Method 3)
*   **File:** `RegistrationServiceImpl.java`
*   **Thay đổi:** Không gọi hàm DB `scheduleRepository.countOverlappingSchedules()` nhiều lần cho mỗi vòng lặp môn học lý thuyết và thực hành.
*   Thay vào đó:
    *   Tải toàn bộ list `Schedule` của các môn sinh viên đã đăng ký trong kỳ lên list memory.
    *   Sử dụng vòng lặp (Java loop) kiểm tra xem ngày học và tiết học của môn học mới có bị đè lên bất kỳ lịch nào trong list memory không. Việc này loại bỏ hoàn toàn các câu lệnh SQL query vô ích.

### 4. Redis Atomic Counter cho Sĩ Số Lớp (Method 4)
*   **File:** `IRedisService.java` & `RedisService.java`.
*   **Thay đổi:**
    *   Thêm hàm `syncClassCapacityToRedis(UUID semesterId)` để đẩy toàn bộ sức chứa (số slot còn trống) của các lớp trong kỳ học lên Redis (vd key: `class_slot:{classId}`).
    *   Thêm hàm `tryAcquireSlot(UUID classSectionId)` chạy một đoạn **Lua Script** trên Redis để an toàn kiểm tra `slot > 0` và tiến hành `DECR`. Lua script đảm bảo không bị quá slot dù có ngàn request đến cùng 1 nano-giây.
    *   Thêm hàm `releaseSlot(UUID classSectionId)` để trả lại slot (bằng lệnh `INCR`) trong trường hợp quá trình Insert DB bị lỗi (ví dụ lỗi Transaction do trùng lịch đột xuất hoặc Data Integrity).
*   **File:** `RegistrationServiceImpl.java`.
    *   Trong `enroll()`, chỉ cho phép đi tiếp nếu `redisService.tryAcquireSlot()` thành công.

## Kế Hoạch Xác Minh (Verification Plan)

### Automated Tests
*   Sử dụng JMeter bắn 1000 requests đăng ký cùng lúc vào 1 mã lớp (cấu hình 50 slot).
*   Kiểm tra số lượng connection CSDL qua HikariCP (số lượng active threads sẽ rất ít).
*   Đảm bảo chính xác chỉ có 50 người đăng ký thành công, số lượng còn lại bị từ chối với tốc độ ngay tức khắc (nhờ Redis).

### Manual Verification
*   Thực hiện đồng bộ dữ liệu bằng API `syncClassCapacityToRedis()`.
*   Tiến hành đăng ký một môn lý thuyết và 1 môn thực hành qua giao diện hoặc Postman.
*   Kiểm tra các validation (tiên quyết, trùng lịch) vẫn ném lỗi đúng, và slot trên Redis phải được hoàn lại (Rollback slot) nếu có lỗi xảy ra ở bước cuối cùng.
