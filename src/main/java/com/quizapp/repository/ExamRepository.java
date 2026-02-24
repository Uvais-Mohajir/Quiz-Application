package com.quizapp.repository;

import com.quizapp.entity.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamRepository extends MongoRepository<Exam, String> {

    List<Exam> findByActiveTrue();


    List<Exam> findByStartTimeBeforeAndEndTimeAfter(
            LocalDateTime now1,
            LocalDateTime now2
    );

    List<Exam> findByMentorIdOrderByStartTimeDesc(String mentorId);

    List<Exam> findByActiveTrueAndEndTimeAfterOrderByStartTimeAsc(LocalDateTime now);
}
