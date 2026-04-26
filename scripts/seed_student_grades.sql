-- ============================================================
-- SEED SCRIPT: Dữ liệu điểm sinh viên kỳ trước (Test Data)
-- Database: PostgreSQL
-- Mô tả: Tạo 1 kỳ học cũ, mở lớp cho các môn hiện có,
--         ghi danh sinh viên, và nhập điểm (có pass / fail)
-- Chạy: psql -d <your_db> -f seed_student_grades.sql
-- ============================================================

BEGIN;

-- ─── BƯỚC 1: Tạo kỳ học cũ (HK1 2023-2024) ─────────────────────────────────
-- Chỉ chèn nếu chưa có kỳ học trùng tên trong năm học đó

-- 1a. Lấy academic_year_id đầu tiên hiện có
DO $$
DECLARE
    v_ay_id          UUID;
    v_semester_id    UUID;
    v_subject_ids    UUID[];
    v_student_ids    UUID[];
    v_sc_id          UUID;  -- subject_component_id
    v_section_id     UUID;
    v_enrollment_id  UUID;
    i                INT;
    j                INT;
    v_mid            NUMERIC(4,1);
    v_final          NUMERIC(4,1);
    v_total          NUMERIC(4,1);
    v_passed         BOOLEAN;
BEGIN

    -- ── 1. Lấy academic_year_id ──────────────────────────────────────────────
    SELECT id INTO v_ay_id FROM academic_years ORDER BY created_at ASC LIMIT 1;

    IF v_ay_id IS NULL THEN
        RAISE NOTICE 'Không tìm thấy academic_year nào. Tạo mới...';
        INSERT INTO academic_years (id, name, start_year, end_year)
        VALUES (gen_random_uuid(), '2023-2024', 2023, 2024)
        RETURNING id INTO v_ay_id;
    END IF;

    RAISE NOTICE 'academic_year_id = %', v_ay_id;

    -- ── 2. Tạo kỳ học cũ (isCurrent = false) ─────────────────────────────────
    SELECT id INTO v_semester_id
    FROM semesters
    WHERE name = 'HK1 2023-2024' AND academic_year_id = v_ay_id;

    IF v_semester_id IS NULL THEN
        INSERT INTO semesters (id, name, academic_year_id, start_date, end_date, is_current)
        VALUES (
            gen_random_uuid(),
            'HK1 2023-2024',
            v_ay_id,
            '2023-09-01',
            '2024-01-15',
            FALSE
        )
        RETURNING id INTO v_semester_id;
        RAISE NOTICE 'Đã tạo semester: HK1 2023-2024 (id = %)', v_semester_id;
    ELSE
        RAISE NOTICE 'Kỳ học HK1 2023-2024 đã tồn tại (id = %)', v_semester_id;
    END IF;

    -- ── 3. Lấy danh sách subjects hiện có (tối đa 5 môn, có subject_component) ─
    SELECT ARRAY(
        SELECT DISTINCT sc.subject_id
        FROM subject_components sc
        JOIN subjects s ON s.id = sc.subject_id
        WHERE s.is_active = TRUE
        LIMIT 5
    ) INTO v_subject_ids;

    IF array_length(v_subject_ids, 1) IS NULL OR array_length(v_subject_ids, 1) = 0 THEN
        RAISE EXCEPTION 'Không tìm thấy môn học nào có subject_component. Vui lòng tạo môn học trước khi chạy script này.';
    END IF;

    RAISE NOTICE 'Tìm thấy % môn học để mở lớp', array_length(v_subject_ids, 1);

    -- ── 4. Lấy danh sách sinh viên (tối đa 5 sinh viên) ─────────────────────
    SELECT ARRAY(
        SELECT id FROM students ORDER BY student_code ASC LIMIT 5
    ) INTO v_student_ids;

    IF array_length(v_student_ids, 1) IS NULL OR array_length(v_student_ids, 1) = 0 THEN
        RAISE EXCEPTION 'Không tìm thấy sinh viên nào trong hệ thống. Vui lòng thêm sinh viên trước.';
    END IF;

    RAISE NOTICE 'Tìm thấy % sinh viên', array_length(v_student_ids, 1);

    -- ── 5. Với mỗi môn học: tạo 1 class_section và ghi danh tất cả sinh viên ─
    FOR i IN 1 .. array_length(v_subject_ids, 1) LOOP

        -- Lấy subject_component đầu tiên của môn (ưu tiên THEORY)
        SELECT id INTO v_sc_id
        FROM subject_components
        WHERE subject_id = v_subject_ids[i]
        ORDER BY
            CASE WHEN type = 'THEORY' THEN 0 ELSE 1 END
        LIMIT 1;

        -- Tạo class_section cho kỳ cũ (nếu chưa có)
        SELECT id INTO v_section_id
        FROM class_sections
        WHERE subject_id = v_subject_ids[i]
          AND semester_id = v_semester_id
          AND parent_section_id IS NULL
        LIMIT 1;

        IF v_section_id IS NULL THEN
            -- Tạo section code dạng <subject_code>-OLD-01
            INSERT INTO class_sections (
                id, section_code, subject_component_id, subject_id,
                semester_id, capacity, min_students, enrolled_count,
                status, created_at, updated_at
            )
            VALUES (
                gen_random_uuid(),
                (SELECT code FROM subjects WHERE id = v_subject_ids[i]) || '-OLD-01',
                v_sc_id,
                v_subject_ids[i],
                v_semester_id,
                50,   -- capacity
                0,    -- min_students
                0,    -- enrolled_count (sẽ update sau)
                'OPENED',
                NOW(),
                NOW()
            )
            RETURNING id INTO v_section_id;

            RAISE NOTICE '  → Tạo class_section cho môn %: section_id = %', v_subject_ids[i], v_section_id;
        ELSE
            RAISE NOTICE '  → Class_section cho môn % đã tồn tại: %', v_subject_ids[i], v_section_id;
        END IF;

        -- ── 6. Ghi danh sinh viên + nhập điểm ──────────────────────────────
        FOR j IN 1 .. array_length(v_student_ids, 1) LOOP

            -- Kiểm tra enrollment đã tồn tại chưa
            SELECT id INTO v_enrollment_id
            FROM enrollments
            WHERE student_id = v_student_ids[j]
              AND class_section_id = v_section_id;

            IF v_enrollment_id IS NULL THEN
                INSERT INTO enrollments (id, student_id, class_section_id, enrollment_date)
                VALUES (gen_random_uuid(), v_student_ids[j], v_section_id, NOW() - INTERVAL '6 months')
                RETURNING id INTO v_enrollment_id;

                -- Tăng enrolled_count
                UPDATE class_sections SET enrolled_count = enrolled_count + 1
                WHERE id = v_section_id;
            END IF;

            -- Sinh điểm ngẫu nhiên đa dạng:
            -- Sinh viên 1,2,3: điểm tốt (pass)
            -- Sinh viên 4: điểm vừa (pass)
            -- Sinh viên 5: điểm thấp (fail)
            CASE
                WHEN j = 1 THEN
                    v_mid   := 7.5 + (i * 0.3)::NUMERIC(4,1);
                    v_final := 8.0 + (i * 0.2)::NUMERIC(4,1);
                WHEN j = 2 THEN
                    v_mid   := 6.0 + (i * 0.5)::NUMERIC(4,1);
                    v_final := 7.0 + (i * 0.3)::NUMERIC(4,1);
                WHEN j = 3 THEN
                    v_mid   := 8.0;
                    v_final := 8.5;
                WHEN j = 4 THEN
                    v_mid   := 5.0;
                    v_final := 5.5;
                ELSE
                    v_mid   := 3.0;
                    v_final := 4.0;
            END CASE;

            -- Giới hạn điểm tối đa 10
            v_mid   := LEAST(v_mid,   10.0);
            v_final := LEAST(v_final, 10.0);

            -- Công thức điểm tổng kết: 30% midterm + 70% final
            v_total   := ROUND(v_mid * 0.3 + v_final * 0.7, 1);
            v_passed  := (v_total >= 5.0);

            -- Chèn hoặc cập nhật student_grade
            INSERT INTO student_grades (id, enrollment_id, midterm_score, final_score, total_score, is_passed)
            VALUES (gen_random_uuid(), v_enrollment_id, v_mid, v_final, v_total, v_passed)
            ON CONFLICT (enrollment_id)
            DO UPDATE SET
                midterm_score = EXCLUDED.midterm_score,
                final_score   = EXCLUDED.final_score,
                total_score   = EXCLUDED.total_score,
                is_passed     = EXCLUDED.is_passed;

            RAISE NOTICE '    SV[%] → môn[%]: midterm=%, final=%, total=%, passed=%',
                j, i, v_mid, v_final, v_total, v_passed;

        END LOOP; -- j (students)

    END LOOP; -- i (subjects)

    RAISE NOTICE '====================================================';
    RAISE NOTICE 'SEED HOÀN TẤT!';
    RAISE NOTICE 'Kỳ học: HK1 2023-2024 (id: %)', v_semester_id;
    RAISE NOTICE 'Số môn học đã mở lớp: %', array_length(v_subject_ids, 1);
    RAISE NOTICE 'Số sinh viên được ghi điểm: %', array_length(v_student_ids, 1);
    RAISE NOTICE '====================================================';

END $$;

COMMIT;

-- ─── KIỂM TRA KẾT QUẢ ────────────────────────────────────────────────────────
-- Chạy các query này sau khi seed để xác nhận dữ liệu

-- 1. Xem tất cả điểm vừa nhập
SELECT
    st.student_code,
    st.full_name                                   AS sinh_vien,
    sub.code                                       AS ma_mon,
    sub.name                                       AS ten_mon,
    sub.credits                                    AS tin_chi,
    sem.name                                       AS ky_hoc,
    sg.midterm_score                               AS diem_gk,
    sg.final_score                                 AS diem_ck,
    sg.total_score                                 AS tong_ket,
    CASE WHEN sg.is_passed THEN 'ĐẠT' ELSE 'TRƯỢT' END AS ket_qua
FROM student_grades sg
JOIN enrollments     e   ON e.id  = sg.enrollment_id
JOIN students        st  ON st.id = e.student_id
JOIN class_sections  cs  ON cs.id = e.class_section_id
JOIN subjects        sub ON sub.id = cs.subject_id
JOIN semesters       sem ON sem.id = cs.semester_id
WHERE sem.name = 'HK1 2023-2024'
ORDER BY st.student_code, sub.code;

-- 2. Kiểm tra passedSubjectIds cho sinh viên đầu tiên
SELECT
    st.student_code,
    sub.code   AS ma_mon_da_qua,
    sub.name   AS ten_mon_da_qua
FROM student_grades sg
JOIN enrollments    e   ON e.id  = sg.enrollment_id
JOIN students       st  ON st.id = e.student_id
JOIN class_sections cs  ON cs.id = e.class_section_id
JOIN subjects       sub ON sub.id = cs.subject_id
WHERE sg.is_passed = TRUE
ORDER BY st.student_code, sub.code;
