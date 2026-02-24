package com.quizapp.mapper;

import com.quizapp.dto.QuestionDTO;
import com.quizapp.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    public Question toEntity(QuestionDTO dto) {
        if (dto == null) {
            return null;
        }
        return Question.builder()
                .id(dto.getQuestionId())
                .examId(dto.getExamId())
                .questionText(dto.getQuestionText())
                .options(dto.getOptions())
                .correctAnswer(dto.getCorrectAnswer())
                .build();
    }

    public QuestionDTO toDTO(Question question, boolean includeCorrectAnswer) {
        if (question == null) {
            return null;
        }
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(question.getId());
        dto.setExamId(question.getExamId());
        dto.setQuestionText(question.getQuestionText());
        dto.setOptions(question.getOptions());
        if (includeCorrectAnswer) {
            dto.setCorrectAnswer(question.getCorrectAnswer());
        }
        return dto;
    }
}
