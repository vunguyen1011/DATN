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
    private final ClassSectionRepository classSectionRepository;
    private final SubjectComponentMapper subjectComponentMapper;

    @Value("${app.semester.default-weeks:15}")
    private int defaultSemesterWeeks;

    @Override
    @Transactional
    public SubjectComponentResponse createSubjectComponent(SubjectComponentRequest request) {
        if (subjectComponentRepository.existsBySubjectIdAndType(request.getSubjectId(), request.getType())) {
            throw new AppException(ErrorCode.COMPONENT_TYPE_ALREADY_EXISTS);
        }

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        int finalTotalPeriods = calculateTotalPeriods(request);

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        SubjectComponent entity = subjectComponentMapper.toEntity(request, subject, roomType);
        entity.setTotalPeriods(finalTotalPeriods);

        SubjectComponent savedEntity = subjectComponentRepository.save(entity);

        // Kích hoạt đồng bộ hóa: Tự động tính và cập nhật tổng số tiết cho Subject
        syncSubjectTotalPeriods(subject);

        return subjectComponentMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public SubjectComponentResponse updateSubjectComponent(UUID id, SubjectComponentRequest request) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));

        Subject subject = entity.getSubject();

        if (subjectComponentRepository.existsBySubjectIdAndTypeAndIdNot(subject.getId(), request.getType(), id)) {
            throw new AppException(ErrorCode.COMPONENT_TYPE_ALREADY_EXISTS);
        }

        int finalTotalPeriods = calculateTotalPeriods(request);

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        subjectComponentMapper.updateEntityFromRequest(entity, request, subject, roomType);
        entity.setTotalPeriods(finalTotalPeriods);

        SubjectComponent savedEntity = subjectComponentRepository.save(entity);

        // Kích hoạt đồng bộ hóa sau khi update
        syncSubjectTotalPeriods(subject);

        return subjectComponentMapper.toResponse(savedEntity);
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

        if (classSectionRepository.existsBySubjectComponentId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể xóa vì đã có Lớp học phần đang sử dụng thành phần này");
        }

        Subject subject = entity.getSubject();

        subjectComponentRepository.delete(entity);

        // Ép JPA thực thi lệnh Delete xuống DB ngay lập tức trước khi query lại để tính tổng
        subjectComponentRepository.flush();

        // Kích hoạt đồng bộ hóa sau khi xóa (Tổng số tiết sẽ bị trừ đi tương ứng)
        syncSubjectTotalPeriods(subject);
    }

    // --- HELPER METHODS ---

    private int calculateTotalPeriods(SubjectComponentRequest request) {
        // Ưu tiên lấy tổng số tiết nếu Frontend truyền thẳng xuống
        if (request.getTotalPeriods() != null && request.getTotalPeriods() > 0) {
            return request.getTotalPeriods();
        }
        // Tự tính dựa trên số tuần mặc định
        if (request.getSessionsPerWeek() != null && request.getPeriodsPerSession() != null) {
            return request.getSessionsPerWeek() * request.getPeriodsPerSession() * defaultSemesterWeeks;
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng cung cấp 'Tổng số tiết' hoặc ('Số buổi/tuần' và 'Số tiết/buổi')");
    }

    private void syncSubjectTotalPeriods(Subject subject) {
        // Tìm toàn bộ các thành phần (Component) hiện đang thuộc về môn học này
        List<SubjectComponent> existingComponents = subjectComponentRepository.findBySubjectId(subject.getId());

        // Cộng dồn toàn bộ số tiết
        int total = existingComponents.stream()
                .mapToInt(comp -> comp.getTotalPeriods() != null ? comp.getTotalPeriods() : 0)
                .sum();

        // Cập nhật và lưu đè lên Môn học
        subject.setTotalPeriods(total);
        subjectRepository.save(subject);
    }
}