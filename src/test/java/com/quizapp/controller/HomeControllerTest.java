package com.quizapp.controller;

import com.quizapp.dto.ExamDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AdminService;
import com.quizapp.service.AuthService;
import com.quizapp.service.ExamService;
import com.quizapp.service.QuestionService;
import com.quizapp.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private AdminService adminService;
    @Mock
    private ExamService examService;
    @Mock
    private QuestionService questionService;
    @Mock
    private StudentService studentService;
    @Mock
    private ResultRepository resultRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HomeController homeController;

    @Test
    void indexPage_setsLiveCountAndReturnsView() {
        ExamDTO e1 = new ExamDTO();
        ExamDTO e2 = new ExamDTO();
        when(examService.getActiveExams()).thenReturn(List.of(e1, e2));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = homeController.indexPage(model);

        assertEquals("index", view);
        assertEquals(2, model.getAttribute("liveTestCount"));
    }

    @Test
    void register_rejectsAdminSelfRegistrationAndRedirectsBack() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = homeController.register("Admin", "admin@x.com", "pass123", Role.ADMIN, redirect);

        assertEquals("redirect:/register", view);
        assertEquals("Admin self-registration is not allowed", redirect.getFlashAttributes().get("error"));
    }

    @Test
    void register_redirectsToOtpVerification() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = homeController.register("P", "p@x.com", "pass123", Role.PARTICIPANT, redirect);

        ArgumentCaptor<com.quizapp.dto.UserDTO> captor = ArgumentCaptor.forClass(com.quizapp.dto.UserDTO.class);
        verify(authService).register(captor.capture());
        assertEquals(Role.PARTICIPANT, captor.getValue().getRole());
        assertEquals("redirect:/verify-otp", view);
    }

    @Test
    void participantHome_marksRegisteredState() {
        ExtendedModelMap model = new ExtendedModelMap();
        User user = User.builder().id("u1").email("p@x.com").build();
        when(examService.getActiveExams()).thenReturn(List.of());
        when(userRepository.findByEmail("p@x.com")).thenReturn(Optional.of(user));
        when(resultRepository.findByStudentId("u1")).thenReturn(List.of());
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("p@x.com");

        String view = homeController.participantHome(auth, model);

        assertEquals("participant-dashboard", view);
        assertEquals(0, ((List<?>) model.getAttribute("participantExamRows")).size());
    }
}
