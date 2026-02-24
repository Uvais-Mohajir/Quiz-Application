package com.quizapp.service;

import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_savesEncodedPasswordAndNotifies() {
        UserDTO dto = new UserDTO();
        dto.setName("P");
        dto.setEmail("p@x.com");
        dto.setPassword("plain");
        dto.setRole(Role.PARTICIPANT);

        User mapped = User.builder().name("P").email("p@x.com").role(Role.PARTICIPANT).build();
        when(userRepository.existsByEmail("p@x.com")).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mapped);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u1");
            return saved;
        });

        authService.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals(true, captor.getValue().isApproved());
        assertEquals(false, captor.getValue().isEmailVerified());
        assertTrue(captor.getValue().getEmailOtp() != null && captor.getValue().getEmailOtp().length() == 6);
        verify(notificationService).notifyRegistrationOtp(captor.getValue(), captor.getValue().getEmailOtp());
    }

    @Test
    void register_throwsWhenEmailExists() {
        UserDTO dto = new UserDTO();
        dto.setEmail("dup@x.com");
        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(dto));
    }

    @Test
    void verifyRegistrationOtp_marksUserVerified() {
        User user = User.builder()
                .id("u1")
                .email("u@x.com")
                .name("User")
                .emailVerified(false)
                .emailOtp("123456")
                .emailOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("u@x.com")).thenReturn(java.util.Optional.of(user));

        authService.verifyRegistrationOtp("u@x.com", "123456");

        assertEquals(true, user.isEmailVerified());
        assertEquals(null, user.getEmailOtp());
        verify(userRepository).save(user);
        verify(notificationService).notifyEmailVerified(user);
    }
}
