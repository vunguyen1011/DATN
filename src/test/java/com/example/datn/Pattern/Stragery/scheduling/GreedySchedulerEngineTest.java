package com.example.datn.Pattern.Stragery.scheduling;

import com.example.datn.DTO.Response.AutoAssignResultResponse;
import com.example.datn.Model.*;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.RoomRepository;
import com.example.datn.Repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreedySchedulerEngineTest {

    @Mock
    private SchedulingMatrixBuilder matrixBuilder;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ClassSectionRepository classSectionRepository;

    @InjectMocks
    private GreedySchedulerEngine greedySchedulerEngine;

    private UUID semesterId;

    @BeforeEach
    void setUp() {
        semesterId = UUID.randomUUID();
    }

    @Test
    void testRun_SuccessWithElearningAndRegular() {
        // Arrange
        RoomType elearningType = new RoomType();
        elearningType.setId(UUID.fromString("f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c")); // ELEARNING_TYPE_ID
        
        RoomType normalType = new RoomType();
        normalType.setId(UUID.randomUUID());
        normalType.setCode("THEORY");
        
        Room room1 = new Room();
        room1.setId(UUID.randomUUID());
        room1.setCapacity(50);
        room1.setRoomType(normalType);

        when(roomRepository.findAll()).thenReturn(new ArrayList<>(List.of(room1)));

        Schedule elearningSchedule = createSchedule(elearningType, 50, 3);
        Schedule normalSchedule = createSchedule(normalType, 30, 3);

        Page<Schedule> schedulePage = new PageImpl<>(List.of(elearningSchedule, normalSchedule));
        when(scheduleRepository.findBySemesterId(eq(semesterId), any(Pageable.class))).thenReturn(schedulePage);

        when(classSectionRepository.findByParentSectionId(any())).thenReturn(Collections.emptyList());

        SchedulingContext ctx = mock(SchedulingContext.class);
        when(matrixBuilder.build(semesterId)).thenReturn(ctx);
        when(ctx.isRoomBusy(any(), anyInt(), anyInt())).thenReturn(false);
        when(ctx.getMaxConcurrentBySubject()).thenReturn(new HashMap<>());

        // Act
        AutoAssignResultResponse result = greedySchedulerEngine.run(semesterId);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getSuccessRate());
        assertEquals(1, result.getPlaced()); // normal schedule is placed
        assertEquals(0, result.getFailed());
        verify(scheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRun_FailDueToNoRoomAvailable() {
        // Arrange
        RoomType normalType = new RoomType();
        normalType.setId(UUID.randomUUID());
        normalType.setCode("THEORY");
        
        // Setup room with small capacity
        Room room1 = new Room();
        room1.setId(UUID.randomUUID());
        room1.setCapacity(10);
        room1.setRoomType(normalType);

        when(roomRepository.findAll()).thenReturn(new ArrayList<>(List.of(room1)));

        // Setup schedule requiring large capacity
        Schedule normalSchedule = createSchedule(normalType, 50, 3);

        Page<Schedule> schedulePage = new PageImpl<>(List.of(normalSchedule));
        when(scheduleRepository.findBySemesterId(eq(semesterId), any(Pageable.class))).thenReturn(schedulePage);

        when(classSectionRepository.findByParentSectionId(any())).thenReturn(Collections.emptyList());

        SchedulingContext ctx = mock(SchedulingContext.class);
        when(matrixBuilder.build(semesterId)).thenReturn(ctx);

        // Act
        AutoAssignResultResponse result = greedySchedulerEngine.run(semesterId);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getSuccessRate());
        assertEquals(0, result.getPlaced());
        assertEquals(1, result.getFailed());
        assertEquals(GreedySchedulerEngine.ERR_NO_ROOM, result.getFailedSchedules().get(0).getReason());
    }

    private Schedule createSchedule(RoomType requiredRoomType, int capacity, int periods) {
        Schedule schedule = new Schedule();
        schedule.setId(UUID.randomUUID());
        
        ClassSection section = new ClassSection();
        section.setId(UUID.randomUUID());
        section.setSectionCode("SEC-001");
        section.setCapacity(capacity);
        
        Subject subject = new Subject();
        subject.setId(UUID.randomUUID());
        subject.setName("Test Subject");
        section.setSubject(subject);
        
        SubjectComponent comp = new SubjectComponent();
        comp.setId(UUID.randomUUID());
        comp.setRequiredRoomType(requiredRoomType);
        comp.setPeriodsPerSession(periods);
        section.setSubjectComponent(comp);
        
        schedule.setClassSection(section);
        return schedule;
    }
}
