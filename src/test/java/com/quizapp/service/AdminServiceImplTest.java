package com.quizapp.service;

import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsers_mapsEntitiesToDtos() {
        User u = User.builder().id("u1").name("A").build();
        UserDTO dto = new UserDTO();
        dto.setId("u1");
        dto.setName("A");
        when(userRepository.findAll()).thenReturn(List.of(u));
        when(userMapper.toDTO(u)).thenReturn(dto);

        List<UserDTO> result = adminService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getId());
    }

    @Test
    void approveMentor_updatesAndNotifies() {
        User mentor = User.builder().id("m1").role(Role.MENTOR).approved(false).build();
        when(userRepository.findById("m1")).thenReturn(Optional.of(mentor));

        adminService.approveMentor("m1");

        assertEquals(true, mentor.isApproved());
        verify(userRepository).save(mentor);
        verify(notificationService).notifyMentorApproved(mentor);
    }

    @Test
    void approveMentor_throwsForNonMentor() {
        User participant = User.builder().id("u1").role(Role.PARTICIPANT).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(participant));

        assertThrows(BadRequestException.class, () -> adminService.approveMentor("u1"));
    }
}
