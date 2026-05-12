package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectComponentMapper;
import com.example.datn.Model.RoomType;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.SubjectComponentRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Repository.TypeRoomRepository;
import com.example.datn.Service.Interface.ISubjectComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectComponentServiceImpl implements ISubjectComponentService {

    private final SubjectComponentRepository subjectComponentRepository;
    private final SubjectRepository subjectRepository;
    private final TypeRoomRepository roomTypeRepository;
    private final ClassSectionRepository classSectionRepository; // Thêm Repo này để chặn xóa bậy
    private final SubjectComponentMapper subjectComponentMapper;

    // Lấy số tuần học mặc định từ file properties (hoặc mặc định là 15)
    @Value("${app.semester.default-weeks:15}")
    private int defaultSemesterWeeks;

    @Override
    @Transactional
    public SubjectComponentResponse createSubjectComponent(SubjectComponentRequest request) {
        // BƯỚC CHẶN 1: Kiểm tra xem môn học này đã có loại thành phần này chưa
        if (subjectComponentRepository.existsBySubjectIdAndType(request.getSubjectId(), request.getType())) {
            throw new AppException(ErrorCode.COMPONENT_TYPE_ALREADY_EXISTS);
        }

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // Tính toán số tiết mới (Lưu ra biến thay vì sửa trực tiếp vào Request DTO)
        int finalTotalPeriods = calculateTotalPeriods(request);

        // BƯỚC CHẶN 2: Kiểm tra tổng số tiết các thành phần không vượt quá số tiết môn học
        validateComponentPeriods(subject, finalTotalPeriods, null);

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        SubjectComponent entity = subjectComponentMapper.toEntity(request, subject, roomType);
        entity.setTotalPeriods(finalTotalPeriods); // Gán số tiết chuẩn xác đã tính vào Entity

        return subjectComponentMapper.toResponse(subjectComponentRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectComponentResponse updateSubjectComponent(UUID id, SubjectComponentRequest request) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));

        // Tối ưu: Lấy Subject từ Entity cũ trong RAM (Không cần query lại Database)
        Subject subject = entity.getSubject();

        // BƯỚC CHẶN 1: Kiểm tra trùng loại thành phần nhưng bỏ qua bản ghi hiện tại
        if (subjectComponentRepository.existsBySubjectIdAndTypeAndIdNot(subject.getId(), request.getType(), id)) {
            throw new AppException(ErrorCode.COMPONENT_TYPE_ALREADY_EXISTS);
        }

        // Tính toán số tiết mới
        int finalTotalPeriods = calculateTotalPeriods(request);

        // BƯỚC CHẶN 2: Kiểm tra tổng số tiết (Truyền ID hiện tại vào để vòng lặp không cộng dồn số cũ)
        validateComponentPeriods(subject, finalTotalPeriods, id);

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        // Cập nhật Entity
        subjectComponentMapper.updateEntityFromRequest(entity, request, subject, roomType);
        entity.setTotalPeriods(finalTotalPeriods);

        return subjectComponentMapper.toResponse(subjectComponentRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectComponentResponse getSubjectComponentById(UUID id) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));
        return subjectComponentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectComponentResponse> getComponentsBySubjectId(UUID subjectId) {
        List<SubjectComponent> entities = subjectComponentRepository.findBySubjectId(subjectId);
        return subjectComponentMapper.toResponseList(entities);
    }

    @Override
    @Transactional
    public void deleteSubjectComponent(UUID id) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));

        // BƯỚC CHẶN: Không cho phép xóa nếu Thành phần này đã được mang đi mở Lớp học phần
        if (classSectionRepository.existsBySubjectComponentId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể xóa vì đã có Lớp học phần đang sử dụng thành phần này");
        }

        subjectComponentRepository.delete(entity);
    }

    // --- HELPER METHODS (HÀM BỔ TRỢ XỬ LÝ LOGIC) ---

    private int calculateTotalPeriods(SubjectComponentRequest request) {
        // Nếu user truyền sẵn tổng số tiết thì dùng luôn
        if (request.getTotalPeriods() != null && request.getTotalPeriods() > 0) {
            return request.getTotalPeriods();
        }
        // Nếu không, tự tính dựa trên số buổi * số tiết * số tuần
        if (request.getSessionsPerWeek() != null && request.getPeriodsPerSession() != null) {
            return request.getSessionsPerWeek() * request.getPeriodsPerSession() * defaultSemesterWeeks;
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng cung cấp 'Tổng số tiết' hoặc ('Số buổi/tuần' và 'Số tiết/buổi')");
    }

    private void validateComponentPeriods(Subject subject, int newPeriods, UUID excludeComponentId) {
        // Lấy số tiết tối đa của môn học
        // LƯU Ý: Đảm bảo trong model Subject.java của bạn có trường totalPeriods.
        // Nếu dùng Tín chỉ thì sửa lại thành: int subjectTotalPeriods = subject.getCredits() * 15;
        int subjectTotalPeriods = subject.getTotalPeriods();

        List<SubjectComponent> existingComponents = subjectComponentRepository.findBySubjectId(subject.getId());

        int currentSum = 0;
        for (SubjectComponent comp : existingComponents) {
            // Bỏ qua bản ghi đang sửa để tránh cộng dồn
            if (excludeComponentId == null || !comp.getId().equals(excludeComponentId)) {
                currentSum += (comp.getTotalPeriods() != null ? comp.getTotalPeriods() : 0);
            }
        }

        int proposedSum = currentSum + newPeriods;

        if (proposedSum > subjectTotalPeriods) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    String.format("Tổng số tiết các thành phần (%d) vượt quá quy mô của môn học (%d tiết)",
                            proposedSum, subjectTotalPeriods));
        }
    }
}