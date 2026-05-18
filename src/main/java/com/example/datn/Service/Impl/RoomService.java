package com.example.datn.Service.Impl;

import com.example.datn.Model.Room;
import com.example.datn.Repository.RoomRepository;
import com.example.datn.Service.Interface.IRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class RoomService implements IRoomService {
    private final RoomRepository roomRepository;
    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public List<Room> getAvailableRooms(Integer dayOfWeek, Integer startPeriod, Integer endPeriod) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new IllegalArgumentException("dayOfWeek, startPeriod, and endPeriod must not be null");
        }
        if (startPeriod > endPeriod) {
            throw new IllegalArgumentException("startPeriod must be less than or equal to endPeriod");
        }
        return roomRepository.findAvailableRooms(dayOfWeek, startPeriod, endPeriod);
    }
}
