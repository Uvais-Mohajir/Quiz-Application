package com.quizapp.repository;

import com.quizapp.entity.Result;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends MongoRepository<Result, String> {

    boolean existsByExamIdAndStudentId(String examId, String studentId);

    Optional<Result> findByExamIdAndStudentId(String examId, String studentId);

    List<Result> findByStudentId(String studentId);

    List<Result> findByExamId(String examId);

    List<Result> findByExamIdAndSubmittedTrue(String examId);

    List<Result> findBySubmittedFalse();
}
