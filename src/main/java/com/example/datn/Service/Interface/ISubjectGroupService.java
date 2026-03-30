package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SubjectGroupRequest;
import com.example.datn.DTO.Response.SubjectGroupResponse;

import java.util.List;
import java.util.UUID;

public interface ISubjectGroupService {
    SubjectGroupResponse createGroup(SubjectGroupRequest request);
    SubjectGroupResponse updateGroup(UUID id, SubjectGroupRequest request);
    SubjectGroupResponse getGroupById(UUID id);
    List<SubjectGroupResponse> getAllActiveGroups();
//    List<SubjectGroupResponse> getGroupsByProgramId(UUID programId); // Lấy theo khung CT
    void softDeleteGroup(UUID id);
}
