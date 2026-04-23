package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Model.Room;
import com.example.datn.Service.Interface.IRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/rooms")
@RestController
@RequiredArgsConstructor
public class RoomController {
    private final IRoomService roomService;
    @GetMapping
    public ApiResponse<List<Room>> getAll(){
        return ApiResponse.<List<Room>>builder()
                .code(1000)
                .message("Lấy danh sách phòng học thành công")
                .result(roomService.getAllRooms())
                .build();
    }
}
