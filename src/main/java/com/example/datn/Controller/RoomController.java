package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Model.Room;
import com.example.datn.Service.Interface.IRoomService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Room", description = "Quản lý phòng học")
@RequestMapping("/api/rooms")
@RestController
@RequiredArgsConstructor
public class RoomController {
    private final IRoomService roomService;
    @Operation(summary = "Lấy danh sách phòng học")
    @GetMapping
    public ApiResponse<List<Room>> getAll(){
        return ApiResponse.<List<Room>>builder()
                .code(1000)
                .message("Lấy danh sách phòng học thành công")
                .result(roomService.getAllRooms())
                .build();
    }

    @Operation(summary = "Lấy danh sách các phòng học trống tại khung giờ chỉ định")
    @GetMapping("/available")
    public ApiResponse<List<Room>> getAvailableRooms(
            @RequestParam("dayOfWeek") Integer dayOfWeek,
            @RequestParam("startPeriod") Integer startPeriod,
            @RequestParam("endPeriod") Integer endPeriod) {
        return ApiResponse.<List<Room>>builder()
                .code(1000)
                .message("Lấy danh sách phòng trống thành công")
                .result(roomService.getAvailableRooms(dayOfWeek, startPeriod, endPeriod))
                .build();
    }
}
