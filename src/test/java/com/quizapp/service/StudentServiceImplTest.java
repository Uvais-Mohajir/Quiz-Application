package com.quizapp.service;

import com.quizapp.dto.ResultDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Question;
import com.quizapp.entity.Result;
import com.quizapp.entity.User;
import com.quizapp.mapper.ResultMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ResultRepository resultRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ResultMapper resultMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void submitExam_calculatesScoreAndReturnsMappedResult() {
        ResultDTO submit = new ResultDTO();
        submit.setExamId("e1");
        submit.setAnswers(Map.of("q1", "A", "q2", "B"));

        User student = User.builder().id("s1").email("s@x.com").build();
        Result result = Result.builder()
                .examId("e1")
                .studentId("s1")
                .submitted(false)
                .attemptStartAt(LocalDateTime.now().minusMinutes(5))
                .attemptEndAt(LocalDateTime.now().plusMinutes(5))
                .build();
        Question q1 = Question.builder().id("q1").correctAnswer("A").build();
        Question q2 = Question.builder().id("q2").correctAnswer("C").build();
        Exam exam = Exam.builder()
                .id("e1")
                .title("Mock")
                .active(true)
                .startTime(LocalDateTime.now().minusMinutes(30))
                .endTime(LocalDateTime.now().plusMinutes(30))
                .build();
        ResultDTO mapped = new ResultDTO();
        mapped.setExamId("e1");
        mapped.setScore(1);
        mapped.setMessage("Exam submitted successfully");

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(student));
        when(resultRepository.findByExamIdAndStudentId("e1", "s1")).thenReturn(Optional.of(result));
        when(questionRepository.findByExamId("e1")).thenReturn(List.of(q1, q2));
        when(examRepository.findById("e1")).thenReturn(Optional.of(exam));
        when(resultMapper.toSubmissionDTO(result, "Exam submitted successfully")).thenReturn(mapped);

        ResultDTO response = studentService.submitExam(submit, "s@x.com");

        verify(resultRepository).save(result);
        verify(notificationService).notifyExamSubmission(student, exam, 1);
        assertEquals(1, response.getScore());
        assertEquals("e1", response.getExamId());
    }

    @Test
    void getMyResults_returnsMappedDtos() {
        User student = User.builder().id("s1").email("s@x.com").build();
        Result r1 = Result.builder().examId("e1").score(3).build();
        ResultDTO dto1 = new ResultDTO();
        dto1.setExamId("e1");
        dto1.setScore(3);

        when(userRepository.findByEmail("s@x.com")).thenReturn(Optional.of(student));
        when(resultRepository.findByStudentId("s1")).thenReturn(List.of(r1));
        when(examRepository.findById("e1")).thenReturn(Optional.of(Exam.builder().id("e1").title("Mock 1").build()));
        when(resultMapper.toDTO(r1, "Mock 1")).thenReturn(dto1);

        List<ResultDTO> results = studentService.getMyResults("s@x.com");

        assertEquals(1, results.size());
        assertEquals("e1", results.get(0).getExamId());
    }
}
