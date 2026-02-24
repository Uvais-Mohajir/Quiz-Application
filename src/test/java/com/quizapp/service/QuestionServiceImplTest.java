package com.quizapp.service;

import com.quizapp.dto.QuestionDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Question;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.mapper.QuestionMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Test
    void addQuestion_savesMappedEntity() {
        QuestionDTO dto = new QuestionDTO();
        dto.setExamId("e1");
        dto.setQuestionText("Q1");

        User creator = User.builder().id("m1").approved(true).build();
        Exam exam = Exam.builder().id("e1").mentorId("m1").endTime(LocalDateTime.now().plusHours(2)).build();
        Question entity = Question.builder().questionText("Q1").build();

        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(creator));
        when(examRepository.findById("e1")).thenReturn(Optional.of(exam));
        when(questionMapper.toEntity(dto)).thenReturn(entity);

        questionService.addQuestion(dto, "mentor@x.com");

        assertEquals("e1", entity.getExamId());
        verify(questionRepository).save(entity);
    }

    @Test
    void addQuestion_throwsWhenCreatorBlocked() {
        QuestionDTO dto = new QuestionDTO();
        dto.setExamId("e1");
        when(userRepository.findByEmail("mentor@x.com")).thenReturn(Optional.of(User.builder().approved(false).build()));

        assertThrows(BadRequestException.class, () -> questionService.addQuestion(dto, "mentor@x.com"));
    }

    @Test
    void getQuestionsByExam_mapsWithoutAnswerKey() {
        Question q = Question.builder().id("q1").examId("e1").questionText("Q?").build();
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId("q1");
        when(questionRepository.findByExamId("e1")).thenReturn(List.of(q));
        when(questionMapper.toDTO(q, false)).thenReturn(dto);

        List<QuestionDTO> result = questionService.getQuestionsByExam("e1");

        assertEquals(1, result.size());
        assertEquals("q1", result.get(0).getQuestionId());
    }
}
