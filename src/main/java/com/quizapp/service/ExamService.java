package com.quizapp.service;

import com.quizapp.dto.ExamDTO;

import java.util.List;

public interface ExamService {

    ExamDTO createExam(ExamDTO examDTO, String creatorEmail);

    List<ExamDTO> getActiveExams();

    List<ExamDTO> getExamsByMentor(String mentorEmail);

    ExamDTO getMentorExamById(String examId, String mentorEmail);
}
