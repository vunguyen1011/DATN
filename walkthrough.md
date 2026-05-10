# Kiến Trúc Hệ Thống Đăng Ký Học Phần — Toàn Bộ Luồng

## Tổng quan: Tại sao cần nhiều tầng bảo vệ?

Hệ thống phải xử lý **hàng nghìn sinh viên đồng thời** tranh nhau slot còn lại.
Mỗi tầng giải quyết đúng **1 vấn đề cụ thể**:

| Tầng | Vấn đề giải quyết |
|------|-------------------|
| **Sentinel** | Server bị DDoS hoặc traffic đột biến → fast-fail ngay tại cổng |
| **Redis Lua** | Race condition giành slot → atomic, không thể ngắt giữa chừng |
| **Cache (Caffeine + Redis)** | Giảm DB query cho dữ liệu tĩnh (tiên quyết, lịch, đợt đăng ký) |
| **Async Thread Pool** | HTTP thread không chờ DB → trả response ngay |
| **Native SQL Upsert** | `@Version` conflict & unique constraint race condition |
| **Retry + Backoff** | Xung đột thoáng qua (transient) tự phục hồi |
| **Cache Eviction Bean** | Dữ liệu cache không bị stale sau khi enroll/cancel |

---

## Sơ đồ luồng tổng thể

```
HTTP Request (POST /enroll)
        │
        ▼
╔══════════════════════════════════╗
║  TẦNG 1: SENTINEL RATE LIMIT     ║  ← Giới hạn 2000 QPS toàn hệ thống
║  SentinelConfig                  ║    Vượt ngưỡng → 429 Too Many Requests
╚══════════════════╦═══════════════╝
                   │ OK (dưới 2000 QPS)
                   ▼
╔══════════════════════════════════╗
║  TẦNG 2: VALIDATION              ║  ← Kiểm tra nghiệp vụ, không giữ lock
║  RegistrationServiceImpl         ║
╚══════════════════╦═══════════════╝
                   │ Hợp lệ
                   ▼
╔══════════════════════════════════╗
║  TẦNG 3: CACHE (đọc dữ liệu)    ║  ← Caffeine: tiên quyết, lịch, đợt đăng ký
║  CacheConfig (Caffeine + Redis)  ║    Không cần query DB nếu đã cache
╚══════════════════╦═══════════════╝
                   │ Data OK
                   ▼
╔══════════════════════════════════╗
║  TẦNG 4: REDIS LUA SCRIPT        ║  ← Giành slot atomic
║  IRedisService.tryAcquireSlot()  ║    1=OK / 0=đã đăng ký / -1=hết chỗ
╚══════════════════╦═══════════════╝
                   │ Slot = 1 (thành công)
        ┌──────────┴──────────┐
        ▼                     ▼
╔═══════════════╗  ╔══════════════════════════════════╗
║ TRẢ RESPONSE  ║  ║  TẦNG 5: ASYNC SAVE              ║
║ NGAY CHO SV   ║  ║  AsyncEnrollmentPersister         ║
║ (HTTP done)   ║  ║  → dbSaveExecutor (20-50 threads) ║
╚═══════════════╝  ╚══════════════╦═══════════════════╝
                                  │
                                  ▼
                   ╔══════════════════════════════════╗
                   ║  TẦNG 6: RETRY + NATIVE SQL      ║
                   ║  EnrollmentSaveHelper             ║
                   ║  ├─ native SQL: enrolled_count++  ║
                   ║  └─ native SQL: UPSERT enrollment ║
                   ╚══════════════╦═══════════════════╝
                                  │
                                  ▼
                   ╔══════════════════════════════════╗
                   ║  TẦNG 7: CACHE EVICTION          ║
                   ║  EnrollmentCacheManager           ║
                   ║  evictEnrolledSections()          ║
                   ╚══════════════════════════════════╝
```

---

## Chi tiết từng tầng

---

### TẦNG 1 — Sentinel Rate Limiting
**File**: [`SentinelConfig.java`](file:///d:/datn/src/main/java/com/example/datn/Config/SentinelConfig.java)

**Công dụng**: Chặn traffic vượt ngưỡng **trước khi** request vào bất kỳ business logic nào.
Nếu không có tầng này, server sẽ nhận toàn bộ traffic, connection pool DB cạn kiệt, hệ thống sụp đổ.

```java
// initFlowRules() - chạy lúc khởi động @PostConstruct
FlowRule rule = new FlowRule();
rule.setResource("enrollment_api");          // Tên resource cần bảo vệ
rule.setGrade(RuleConstant.FLOW_GRADE_QPS);  // Giới hạn theo QPS (request/giây)
rule.setCount(2000);                         // Tối đa 2000 request/giây
FlowRuleManager.loadRules(rules);
```

**Cách hoạt động**:
- `FLOW_GRADE_QPS = 2000`: Trong 1 giây chỉ cho phép 2000 request qua
- Request thứ 2001 trở đi → Sentinel ném `BlockException` → trả `429 Too Many Requests`
- **Fast-fail**: không tốn tài nguyên xử lý request dư thừa

**`SentinelResourceAspect` bean**: Đăng ký AspectJ để annotation `@SentinelResource` hoạt động.
Sentinel standalone (không có Spring Cloud) cần bean này để intercept các method được đánh dấu.

> **Lưu ý**: `@SentinelResource("enrollment_api")` cần được đặt trên method `enroll()` trong controller hoặc service để Sentinel biết đây là resource cần bảo vệ.

---

### TẦNG 2 — Validation (không lock)
**File**: [`RegistrationServiceImpl.java`](file:///d:/datn/src/main/java/com/example/datn/Service/Impl/RegistrationServiceImpl.java)

**Công dụng**: Loại bỏ request không hợp lệ sớm nhất có thể — **trước khi** giữ bất kỳ lock nào.

| Method | Làm gì | Cache? |
|--------|--------|--------|
| `getCurrentStudent()` | Lấy info từ JWT token | ❌ (từ token, không cần DB) |
| `getActivePeriodCohort()` | Kiểm tra đang trong đợt đăng ký | ✅ `ongoingCohortPeriod` |
| `validateClassBasics()` | Lớp học phần hợp lệ? | ✅ `classSection` |
| `validateAndGetLabSection()` | Lớp thực hành có thuộc lý thuyết? | ✅ `classSection` |
| `validateDuplicateSubject()` | Đã đăng ký môn này chưa? | ✅ `enrolledSections` |
| `checkPrerequisitesOptimized()` | Đã qua môn tiên quyết chưa? | ✅ `prerequisites` + `passedSubjects` |
| `validateSchedulesWithBatchQuery()` | Trùng lịch? (1 batch query, so sánh RAM) | ✅ `scheduleOverlap` |

```java
// prepareEnrollment() — tạo Enrollment object TRONG HTTP thread (còn session)
Enrollment enrollment = enrollmentRepository
    .findByStudentIdAndClassSectionId(student.getId(), section.getId())
    .orElseGet(() -> {
        Enrollment newEn = Enrollment.builder()...build();
        newEn.setId(UUID.randomUUID()); // ← cấp phát UUID ngay để trả response ngay
        return newEn;
    });
```

```java
// ⚠️ QUAN TRỌNG: Convert DTO ngay trong HTTP thread (session còn sống)
List<EnrollmentSaveRequest> saveRequests = toSave.stream()
        .map(EnrollmentSaveRequest::from)  // ← trích UUID + primitives
        .collect(Collectors.toList());
// SAU DÒNG NÀY: entity đi qua thread pool, session đóng → không đụng entity nữa
```

---

### TẦNG 3 — Cache hai tầng (Caffeine + Redis)
**File**: [`CacheConfig.java`](file:///d:/datn/src/main/java/com/example/datn/Config/CacheConfig.java)

**Công dụng**: Giảm DB query cho dữ liệu ít thay đổi, tăng throughput validation.

#### Caffeine (in-memory, `@Primary`)
Nhanh nhất. Không cần serialize, tránh lỗi `HibernateProxy + Jackson`.

| Cache name | Lưu gì | TTL |
|------------|--------|-----|
| `ongoingCohortPeriod` | Đợt đăng ký hiện tại của cohort | 60 phút |
| `prerequisites` | Danh sách môn tiên quyết của môn X | 60 phút |
| `registrationStatus` | Trạng thái đợt đăng ký | 60 phút |
| `classSection` | Thông tin lớp học phần (tĩnh trong kỳ) | 60 phút |
| `enrolledSections` | Danh sách lớp đã đăng ký của SV | 60 phút |
| `passedSubjects` | Các môn SV đã qua (rất tĩnh) | 60 phút |
| `scheduleOverlap` | Kết quả kiểm tra trùng lịch | 60 phút |

#### Redis Cache
Dùng cho data cần **chia sẻ giữa nhiều instance** server (scale-out).
Serialize qua `GenericJackson2JsonRedisSerializer`, TTL 1 giờ.

> **Tại sao tách hai loại cache?**
> Caffeine: nhanh hơn Redis (in-process, không network). Dùng cho JPA entity tránh serialize lỗi.
> Redis: persistent hơn, chia sẻ được giữa nhiều pod khi deploy multi-instance.

---

### TẦNG 4 — Redis Lua Script (Atomic Slot Management)
**File**: [`IRedisService`](file:///d:/datn/src/main/java/com/example/datn/Service/Interface/IRedisService.java) / `RedisServiceImpl`

**Công dụng**: Giải quyết race condition khi nhiều sinh viên cùng giành slot cuối cùng.

#### `syncClassCapacityToRedis(semesterId)`
> **PHẢI GỌI TRƯỚC KHI MỞ ĐỢT ĐĂNG KÝ** — sync số chỗ còn lại từ DB lên Redis.

#### `tryAcquireSlot(classSectionId, studentId)` → Lua Script
```
Tại sao Lua?
─────────────────────────────────────────────────────────
❌ Không dùng Lua:              ✅ Dùng Lua Script:
GET remaining          ←┐      EVAL script (atomic):
if remaining > 0       ←┤      {
  DECR remaining         │        GET remaining
  SET enrolled[sv]       │        if remaining > 0
                      ←─┘           DECR remaining
  ↑ Khoảng hở này!                  SET enrolled[sv]
    2 luồng cùng                     return 1
    vào → -1 slot!              else return -1
                               }
─────────────────────────────────────────────────────────
```

**Giá trị trả về:**

| Giá trị | Ý nghĩa | Action |
|---------|---------|--------|
| `1` | Thành công, đã giành slot | Tiếp tục async save |
| `0` | SV đã đăng ký lớp này rồi | Throw exception ngay |
| `-1` | Lớp hết chỗ | Throw exception ngay |

#### `releaseSlot(classSectionId, studentId)`
Hoàn trả slot khi async save **thất bại hoàn toàn** sau 3 lần retry.
Không gọi nếu save thành công.

---

### TẦNG 5 — Async Thread Pool
**File**: [`AsyncConfig.java`](file:///d:/datn/src/main/java/com/example/datn/Config/AsyncConfig.java) + [`AsyncEnrollmentPersister.java`](file:///d:/datn/src/main/java/com/example/datn/Service/Impl/AsyncEnrollmentPersister.java)

**Công dụng**: HTTP thread trả response ngay sau khi giành slot Redis — không chờ DB ghi xong.

#### `dbSaveExecutor` bean (AsyncConfig)

| Tham số | Giá trị | Ý nghĩa |
|---------|---------|---------|
| `corePoolSize` | 20 | Luôn có 20 thread sẵn sàng |
| `maxPoolSize` | 50 | Tăng tối đa 50 thread khi tải cao |
| `queueCapacity` | 5000 | Buffer 5000 task khi thread bận |
| `CallerRunsPolicy` | — | Queue đầy → HTTP thread tự chạy (không reject) |
| `ThreadNamePrefix` | `EnrollmentAsync-` | Dễ nhận ra trong log (`EnrollmentAsync-2`) |

#### `saveToDatabaseAsync(requests, isEnroll)` — AsyncEnrollmentPersister

```
@Async("dbSaveExecutor") — chạy trên thread pool, HTTP thread đã return rồi

Với mỗi EnrollmentSaveRequest trong list:
┌─────────────────────────────────────────────────────┐
│  attempt 1 → saveOne() → OK? → done ✓               │
│  attempt 1 → saveOne() → FAIL → wait 50ms           │
│  attempt 2 → saveOne() → OK? → done ✓               │
│  attempt 2 → saveOne() → FAIL → wait 100ms          │
│  attempt 3 → saveOne() → FAIL                       │
│    → LOG ERROR                                       │
│    → releaseSlot() ← hoàn Redis slot về             │
└─────────────────────────────────────────────────────┘
```

**Exponential backoff**: `Thread.sleep(50ms * attempt)` — tránh retry storm (nhiều luồng cùng thử lại cùng lúc).

**Bắt đúng 3 exception**:
- `ObjectOptimisticLockingFailureException` — `@Version` conflict
- `DataIntegrityViolationException` — unique constraint
- `StaleObjectStateException` — entity bị detached (đã fix bằng DTO)

---

### TẦNG 6 — Native SQL + Upsert (EnrollmentSaveHelper)
**File**: [`EnrollmentSaveHelper.java`](file:///d:/datn/src/main/java/com/example/datn/Service/Impl/EnrollmentSaveHelper.java)

**Công dụng**: Ghi DB an toàn, không bị ảnh hưởng bởi `@Version` hay unique conflict.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
// ↑ Mỗi enrollment = 1 transaction độc lập
// Nếu dùng REQUIRED, tất cả cùng 1 transaction → 1 fail → rollback tất cả
```

#### Bước 1: Cập nhật sĩ số — `ClassSectionRepository`

```sql
-- nativeQuery = true → Hibernate KHÔNG tăng @Version của ClassSection
UPDATE class_sections
SET enrolled_count = enrolled_count + 1
WHERE id = :id AND enrolled_count < capacity
```

> **Tại sao native SQL?**
> `ClassSection` có `@Version Long version`. JPQL `UPDATE ClassSection cs SET...`
> → Hibernate tự động `UPDATE class_sections SET version = version+1`
> → 2 luồng cùng update cùng ClassSection → luồng sau thấy version thay đổi
> → `ObjectOptimisticLockingFailureException`
> Native SQL bỏ qua hoàn toàn Hibernate version management.

#### Bước 2: Upsert enrollment — `EnrollmentRepository`

```sql
-- nativeQuery = true, idempotent
INSERT INTO enrollments (id, student_id, class_section_id, status, enrollment_date)
VALUES (:id, :studentId, :classSectionId, :status, :enrollmentDate)
ON CONFLICT (student_id, class_section_id)           -- ← PostgreSQL syntax
DO UPDATE SET status      = EXCLUDED.status,
              enrollment_date = EXCLUDED.enrollment_date
```

> **Tại sao upsert?**
> `enrollments` có `UNIQUE(student_id, class_section_id)`.
> Nếu 2 luồng cùng INSERT cùng (student, class) → luồng sau bị `DataIntegrityViolationException`.
> `ON CONFLICT DO UPDATE` = idempotent: gọi 10 lần vẫn cho kết quả đúng.

> **Lưu ý**: `ON CONFLICT` là PostgreSQL syntax. MySQL dùng `INSERT ... ON DUPLICATE KEY UPDATE`.

---

### TẦNG 7 — Cache Eviction
**File**: [`EnrollmentCacheManager.java`](file:///d:/datn/src/main/java/com/example/datn/Config/EnrollmentCacheManager.java)

**Công dụng**: Xóa cache `enrolledSections` sau khi enroll/cancel để lần đọc tiếp theo lấy data mới.

```java
@CacheEvict(value = "enrolledSections", key = "#studentId + ':' + #semesterId")
public void evictEnrolledSections(UUID studentId, UUID semesterId) {
    // Thân rỗng — Spring AOP tự xử lý eviction
}
```

> **Tại sao bean riêng?**
> Spring Cache dùng AOP Proxy. Nếu `RegistrationServiceImpl` tự gọi
> `this.evictEnrolledSections()` (self-invocation), Spring **không đi qua proxy**
> → `@CacheEvict` bị bỏ qua hoàn toàn, cache không bao giờ xóa → data stale.
> Bean riêng = gọi qua proxy = `@CacheEvict` hoạt động đúng.

---

## Bản đồ file — Vai trò & Method quan trọng

```
Config/
├── SentinelConfig.java
│   ├── sentinelResourceAspect()    → Enable @SentinelResource annotation
│   └── initFlowRules()             → Đặt giới hạn 2000 QPS cho "enrollment_api"
│
├── AsyncConfig.java
│   └── dbSaveExecutor()            → Thread pool 20-50 threads, queue 5000
│
├── CacheConfig.java
│   ├── caffeineCacheManager()      → In-memory cache (Primary), 7 cache regions
│   └── redisCacheManager()         → Redis cache, dùng cho multi-instance
│
└── EnrollmentCacheManager.java
    └── evictEnrolledSections()     → Xóa cache sau enroll/cancel (bean riêng tránh self-invoke)

Service/Interface/
└── IRedisService.java
    ├── syncClassCapacityToRedis()  → Sync slot DB→Redis trước khi mở đợt
    ├── tryAcquireSlot()            → Lua script: giành slot atomic (1/0/-1)
    └── releaseSlot()               → Hoàn slot khi save fail hoàn toàn

Service/Impl/
├── RegistrationServiceImpl.java
│   ├── enroll()                    → Điều phối 5 giai đoạn
│   ├── cancelEnrollment()          → Hủy đăng ký + async save
│   ├── getCurrentStudent()         → Lấy SV từ JWT (không query DB)
│   ├── prepareEnrollment()         → Tạo/tìm Enrollment, cấp UUID ngay
│   ├── validateSchedulesWith...()  → 1 batch query + so sánh RAM
│   └── checkPrerequisitesOpt...()  → Dùng cache passedSubjects
│
├── AsyncEnrollmentPersister.java
│   └── saveToDatabaseAsync()       → @Async, retry 3 lần, exp backoff
│
└── EnrollmentSaveHelper.java
    └── saveOne()                   → @Transactional(REQUIRES_NEW), 2 native SQL

Repository/
├── ClassSectionRepository.java
│   ├── tryIncrementEnrolledCount() → Native SQL UPDATE (bypass @Version)
│   └── tryDecrementEnrolledCount() → Native SQL UPDATE (bypass @Version)
│
└── EnrollmentRepository.java
    └── upsertEnrollment()          → Native SQL INSERT ON CONFLICT DO UPDATE

DTO/
└── EnrollmentSaveRequest.java      → Record thuần túy (UUID + primitives)
    └── from(Enrollment e)          → Factory: entity → DTO trong HTTP thread
```

---

## Bảng tóm tắt: Vấn đề → Giải pháp

| Vấn đề | Giải pháp | File |
|--------|-----------|------|
| Traffic đột biến / DDoS | Sentinel 2000 QPS fast-fail | `SentinelConfig` |
| Race condition giành slot | Redis Lua Script (atomic) | `IRedisService` |
| N+1 query lúc validation | Cache Caffeine (7 regions) | `CacheConfig` |
| HTTP thread chờ DB → timeout | `@Async` thread pool | `AsyncConfig`, `AsyncEnrollmentPersister` |
| `StaleObjectStateException` | Truyền DTO thay entity qua thread | `EnrollmentSaveRequest` |
| `ObjectOptimisticLockingFailureException` | Native SQL bypass `@Version` | `ClassSectionRepository` |
| `DataIntegrityViolationException` (unique) | Native upsert `ON CONFLICT` | `EnrollmentRepository` |
| Xung đột thoáng qua (transient) | Retry 3 lần + exponential backoff | `AsyncEnrollmentPersister` |
| `@CacheEvict` bị bỏ qua | Bean riêng tránh self-invocation | `EnrollmentCacheManager` |
| Slot Redis bị giữ khi save fail | `releaseSlot()` sau retry thất bại | `AsyncEnrollmentPersister` |
