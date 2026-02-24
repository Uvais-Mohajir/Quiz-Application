package com.quizapp.service;

import com.quizapp.dto.ExamDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.Exam;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.mapper.ExamMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ExamMapper examMapper;

    @InjectMocks
    private ExamServiceImpl examService;

    @Test
    void createExam_savesAndReturnsMappedDto() {
        ExamDTO request = new ExamDTO();
        request.setTitle("Mock 1");
        request.setDurationMinutes(60);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        User creator = User.builder().id("m1").approved(true).role(Role.MENTOR).build();
        Exam entity = Exam.builder().title("Mock 1").mentorId("m1").build();
        ExamDTO response = new ExamDTO();
        response.setTitle("Mock 1");

        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(creator));
        when(examMapper.toEntity(request, "m1")).thenReturn(entity);
        when(examMapper.toDTO(entity)).thenReturn(response);

        ExamDTO result = examService.createExam(request, "mentor@x.com");

        ArgumentCaptor<Exam> captor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(captor.capture());
        assertEquals("m1", captor.getValue().getMentorId());
        assertEquals("Mock 1", result.getTitle());
    }

    @Test
    void createExam_throwsWhenCreatorBlocked() {
        ExamDTO request = new ExamDTO();
        request.setDurationMinutes(60);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        User creator = User.builder().approved(false).role(Role.MENTOR).build();
        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(creator));

        assertThrows(BadRequestException.class, () -> examService.createExam(request, "mentor@x.com"));
    }

    @Test
    void getActiveExams_mapsAllEntities() {
        Exam e1 = Exam.builder().id("e1").build();
        Exam e2 = Exam.builder().id("e2").build();
        ExamDTO d1 = new ExamDTO();
        d1.setExamId("e1");
        ExamDTO d2 = new ExamDTO();
        d2.setExamId("e2");

        when(examRepository.findByActiveTrue()).thenReturn(List.of(e1, e2));
        when(questionRepository.findByExamId("e1")).thenReturn(List.of(
                com.quizapp.entity.Question.builder().id("q1").build(),
                com.quizapp.entity.Question.builder().id("q2").build(),
                com.quizapp.entity.Question.builder().id("q3").build()
        ));
        when(questionRepository.findByExamId("e2")).thenReturn(List.of(
                com.quizapp.entity.Question.builder().id("q4").build(),
                com.quizapp.entity.Question.builder().id("q5").build(),
                com.quizapp.entity.Question.builder().id("q6").build()
        ));
        when(examMapper.toDTO(e1)).thenReturn(d1);
        when(examMapper.toDTO(e2)).thenReturn(d2);

        List<ExamDTO> result = examService.getActiveExams();

        assertEquals(2, result.size());
        assertEquals("e1", result.get(0).getExamId());
    }

    @Test
    void getExamsByMentor_returnsMappedExams() {
        User mentor = User.builder().id("m1").role(Role.MENTOR).build();
        Exam exam = Exam.builder().id("e1").mentorId("m1").build();
        ExamDTO dto = new ExamDTO();
        dto.setExamId("e1");

        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(mentor));
        when(examRepository.findByMentorIdOrderByStartTimeDesc("m1")).thenReturn(List.of(exam));
        when(examMapper.toDTO(exam)).thenReturn(dto);

        List<ExamDTO> result = examService.getExamsByMentor("mentor@x.com");

        assertEquals(1, result.size());
        assertEquals("e1", result.get(0).getExamId());
    }
}
