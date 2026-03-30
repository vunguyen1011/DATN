package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.TypeRoomRequest;
import com.example.datn.Model.RoomType;

import java.util.List;
import java.util.UUID;

public interface ITypeRoomService {
    RoomType createTypeRoom(TypeRoomRequest request);
    RoomType updateTypeRoom(UUID id, TypeRoomRequest request);
    void deleteTypeRoom(UUID id);
    RoomType getTypeRoomById(UUID id);
    List<RoomType> getAllTypeRooms();
}
