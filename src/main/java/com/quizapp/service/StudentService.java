package com.quizapp.service;

import com.quizapp.dto.ResultDTO;

import java.util.List;

public interface StudentService {

    void registerForExam(String examId, String studentEmail);

    ResultDTO submitExam(ResultDTO resultDTO, String studentEmail);

    List<ResultDTO> getMyResults(String studentEmail);

    long getRemainingSeconds(String examId, String studentEmail);
}
