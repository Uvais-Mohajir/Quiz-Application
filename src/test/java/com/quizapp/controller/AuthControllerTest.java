package com.quizapp.controller;

import com.quizapp.dto.ApiResponse;
import com.quizapp.dto.LoginDTO;
import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.UnauthorizedException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthController authController;

    @Test
    void userRegister_setsParticipantRole() {
        UserDTO request = new UserDTO();
        request.setName("A");
        request.setEmail("a@x.com");
        request.setPassword("secret123");

        ResponseEntity<ApiResponse<Void>> response = authController.userRegister(request);

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(authService).register(captor.capture());
        assertEquals(Role.PARTICIPANT, captor.getValue().getRole());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void mentorRegister_setsMentorRole() {
        UserDTO request = new UserDTO();
        request.setName("M");
        request.setEmail("m@x.com");
        request.setPassword("secret123");

        authController.mentorRegister(request);

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(authService).register(captor.capture());
        assertEquals(Role.MENTOR, captor.getValue().getRole());
    }

    @Test
    void adminLogin_returnsSessionIdTokenForValidCredentials() {
        LoginDTO login = new LoginDTO();
        login.setEmail("admin@x.com");
        login.setPassword("pass");

        Authentication auth = new UsernamePasswordAuthenticationToken("admin@x.com", "pass");
        User user = User.builder()
                .id("u1")
                .name("Admin")
                .email("admin@x.com")
                .role(Role.ADMIN)
                .approved(true)
                .emailVerified(true)
                .build();
        UserDTO mapped = new UserDTO();
        mapped.setId("u1");
        mapped.setRole(Role.ADMIN);
        mapped.setEmail("admin@x.com");

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(mapped);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("session-1");

        ApiResponse<UserDTO> response = authController.adminLogin(login, httpServletRequest);

        assertEquals(true, response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("session-1", response.getData().getToken());
        verify(httpSession).setAttribute(any(String.class), any());
    }

    @Test
    void userLogin_throwsWhenRoleDoesNotMatchEndpoint() {
        LoginDTO login = new LoginDTO();
        login.setEmail("mentor@x.com");
        login.setPassword("pass");

        Authentication auth = new UsernamePasswordAuthenticationToken("mentor@x.com", "pass");
        User user = User.builder()
                .email("mentor@x.com")
                .role(Role.MENTOR)
                .approved(true)
                .emailVerified(true)
                .build();

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authController.userLogin(login, httpServletRequest));
    }

    @Test
    void mentorLogin_throwsWhenCredentialsInvalid() {
        LoginDTO login = new LoginDTO();
        login.setEmail("mentor@x.com");
        login.setPassword("pass");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(UnauthorizedException.class, () -> authController.mentorLogin(login, httpServletRequest));
    }

    @Test
    void verifyOtp_callsService() {
        ApiResponse<Void> response = authController.verifyOtp("a@x.com", "123456");
        verify(authService).verifyRegistrationOtp("a@x.com", "123456");
        assertEquals(true, response.isSuccess());
    }
}
