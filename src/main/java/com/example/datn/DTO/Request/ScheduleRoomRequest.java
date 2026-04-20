package com.example.datn.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * [ADMIN] Xếp phòng học cho một lịch đã có thời gian.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRoomRequest {

    private UUID roomId;

}

