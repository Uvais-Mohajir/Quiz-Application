package com.quizapp.mapper;

import com.quizapp.dto.ResultDTO;
import com.quizapp.entity.Result;
import org.springframework.stereotype.Component;

@Component
public class ResultMapper {

    public ResultDTO toDTO(Result result, String examTitle) {
        if (result == null) {
            return null;
        }
        ResultDTO dto = new ResultDTO();
        dto.setExamId(result.getExamId());
        dto.setExamTitle(examTitle);
        dto.setAnswers(result.getAnswers());
        dto.setScore(result.getScore());
        dto.setSubmitted(result.isSubmitted());
        dto.setSubmittedAt(result.getSubmittedAt());
        dto.setAttemptStartAt(result.getAttemptStartAt());
        dto.setAttemptEndAt(result.getAttemptEndAt());
        return dto;
    }

    public ResultDTO toSubmissionDTO(Result result, String message) {
        if (result == null) {
            return null;
        }
        ResultDTO dto = new ResultDTO();
        dto.setExamId(result.getExamId());
        dto.setAnswers(result.getAnswers());
        dto.setScore(result.getScore());
        dto.setSubmitted(result.isSubmitted());
        dto.setSubmittedAt(result.getSubmittedAt());
        dto.setAttemptStartAt(result.getAttemptStartAt());
        dto.setAttemptEndAt(result.getAttemptEndAt());
        dto.setMessage(message);
        return dto;
    }
}
