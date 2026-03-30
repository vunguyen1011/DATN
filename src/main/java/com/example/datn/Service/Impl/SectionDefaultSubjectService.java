package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SectionDefaultSubjectMapper;
import com.example.datn.Model.SectionDefault;
import com.example.datn.Model.SectionDefaultSubject;
import com.example.datn.Model.Subject;
import com.example.datn.Repository.SectionDefaultRepository;
import com.example.datn.Repository.SectionDefaultSubjectRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Service.Interface.ISectionDefaultSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionDefaultSubjectService implements ISectionDefaultSubjectService {

    private final SectionDefaultSubjectRepository repository;
    private final SectionDefaultRepository sectionDefaultRepository;
    private final SubjectRepository subjectRepository;
    private final SectionDefaultSubjectMapper mapper;

    @Override
    @Transactional
    public SectionDefaultSubjectResponse create(SectionDefaultSubjectRequest request) {
        // 1. Kiểm tra Khối Cam mẫu
        SectionDefault sectionDefault = sectionDefaultRepository.findById(request.getSectionDefaultId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND));

        // 2. Kiểm tra Môn học
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .filter(Subject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 3. Kiểm tra trùng lặp môn học trong khối
        if (repository.existsBySectionDefaultIdAndSubjectIdAndIsActiveTrue(request.getSectionDefaultId(), request.getSubjectId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        // 4. Cập nhật số tín chỉ yêu cầu NẾU đây là khối Bắt Buộc
        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            sectionDefault.setRequiredCredits(currentCredits + subject.getCredits());
            // JPA @Transactional sẽ tự động lưu thay đổi của sectionDefault xuống DB, không cần gọi .save()
        }

        SectionDefaultSubject entity = mapper.toEntity(request, sectionDefault, subject);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SectionDefaultSubjectResponse update(UUID id, SectionDefaultSubjectRequest request) {
        // 1. Lấy thông tin bản ghi hiện tại
        SectionDefaultSubject entity = repository.findById(id)
                .filter(SectionDefaultSubject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_SUBJECT_NOT_FOUND));

        SectionDefault sectionDefault = entity.getSectionDefault();
        Subject oldSubject = entity.getSubject();

        // 2. Lấy thông tin môn học mới
        Subject newSubject = subjectRepository.findById(request.getSubjectId())
                .filter(Subject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 3. Nếu đổi sang môn học khác, phải check xem môn mới đã tồn tại trong khối chưa
        if (!oldSubject.getId().equals(newSubject.getId()) &&
                repository.existsBySectionDefaultIdAndSubjectIdAndIsActiveTrue(sectionDefault.getId(), newSubject.getId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        // 4. Nếu là khối Bắt Buộc và có sự thay đổi môn học, tính toán lại tổng tín chỉ
        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory()) && !oldSubject.getId().equals(newSubject.getId())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            // Công thức: Trừ đi tín chỉ môn cũ, cộng thêm tín chỉ môn mới
            int updatedCredits = currentCredits - oldSubject.getCredits() + newSubject.getCredits();
            sectionDefault.setRequiredCredits(Math.max(0, updatedCredits)); // Đảm bảo không bao giờ bị số âm
        }

        // 5. Cập nhật dữ liệu
        entity.setSubject(newSubject);
        entity.setDefaultSemester(request.getDefaultSemester());

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID sectionDefaultId, UUID subjectId) {
        SectionDefaultSubject entity = repository.findBySectionDefaultIdAndSubjectIdAndIsActiveTrue(sectionDefaultId, subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_SUBJECT_NOT_FOUND));

        SectionDefault sectionDefault = entity.getSectionDefault();
        Subject subject = entity.getSubject();

        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            int updatedCredits = Math.max(0, currentCredits - subject.getCredits());
            sectionDefault.setRequiredCredits(updatedCredits);
        }

        entity.setIsActive(false);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionDefaultSubjectResponse> getBySectionDefaultId(UUID sectionDefaultId) {
        if (!sectionDefaultRepository.existsById(sectionDefaultId)) {
            throw new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND);
        }

        return repository.findBySectionDefaultIdAndIsActiveTrue(sectionDefaultId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

}