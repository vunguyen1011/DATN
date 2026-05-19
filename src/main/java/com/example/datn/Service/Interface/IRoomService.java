package com.example.datn.Service.Interface;

import com.example.datn.Model.Room;

import java.util.List;

import java.util.UUID;

public interface IRoomService {
    List<Room> getAllRooms();
    List<Room> getAvailableRooms(Integer dayOfWeek, Integer startPeriod, Integer endPeriod);
}
