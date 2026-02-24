package com.quizapp.service;

import com.quizapp.dto.QuestionDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    void addQuestion(QuestionDTO questionDTO, String creatorEmail);

    int addQuestionsFromExcel(MultipartFile file, String creatorEmail);

    int addQuestionsFromExcel(MultipartFile file, String creatorEmail, String fixedExamId);

    List<QuestionDTO> getQuestionsByExam(String examId);
}
