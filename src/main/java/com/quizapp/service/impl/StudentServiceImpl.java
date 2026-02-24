package com.quizapp.service.impl;

import com.quizapp.dto.ResultDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Question;
import com.quizapp.entity.Result;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.mapper.ResultMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.NotificationService;
import com.quizapp.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ResultRepository resultRepository;
    private final NotificationService notificationService;
    private final ResultMapper resultMapper;

    @Override
    public void registerForExam(String examId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        LocalDateTime now = LocalDateTime.now();
        if (!exam.isActive()) {
            throw new BadRequestException("Exam is not active");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BadRequestException("Exam registration is closed");
        }
        if (questionRepository.findByExamId(examId).size() < 3) {
            throw new BadRequestException("Exam must have minimum 3 questions before registration");
        }

        if (resultRepository.existsByExamIdAndStudentId(examId, student.getId())) {
            throw new BadRequestException("Already registered for this exam");
        }

        Result result = Result.builder()
                .examId(examId)
                .studentId(student.getId())
                .score(0)
                .submitted(false)
                .answers(Collections.emptyMap())
                .attemptStartAt(null)
                .attemptEndAt(null)
                .build();

        resultRepository.save(result);
        notificationService.notifyExamRegistration(student, exam);
    }

    @Override
    public ResultDTO submitExam(ResultDTO resultDTO, String studentEmail) {
        if (resultDTO == null || resultDTO.getExamId() == null || resultDTO.getExamId().isBlank()) {
            throw new BadRequestException("Exam id is required");
        }
        if (resultDTO.getAnswers() == null) {
            throw new BadRequestException("Answers are required");
        }

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Result result = resultRepository
                .findByExamIdAndStudentId(resultDTO.getExamId(), student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam registration not found"));

        if (result.isSubmitted()) {
            throw new BadRequestException("Exam already submitted");
        }
        Exam exam = examRepository.findById(resultDTO.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        LocalDateTime now = LocalDateTime.now();

        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BadRequestException("Exam has not started yet");
        }
        if (!exam.isActive()) {
            throw new BadRequestException("Exam is not active");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BadRequestException("Exam window is closed");
        }

        if (result.getAttemptStartAt() == null || result.getAttemptEndAt() == null) {
            throw new BadRequestException("Start exam from dashboard before submitting");
        }

        LocalDateTime hardEndTime = exam.getEndTime() == null
                ? result.getAttemptEndAt()
                : result.getAttemptEndAt().isBefore(exam.getEndTime()) ? result.getAttemptEndAt() : exam.getEndTime();
        if (now.isAfter(hardEndTime)) {
            throw new BadRequestException("Exam duration is over");
        }

        List<Question> questions = questionRepository.findByExamId(resultDTO.getExamId());
        if (questions.isEmpty()) {
            throw new BadRequestException("No questions found for this exam");
        }

        int score = 0;
        for (Question question : questions) {
            String submittedAnswer = resultDTO.getAnswers().get(question.getId());
            if (submittedAnswer != null
                    && submittedAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim())) {
                score++;
            }
        }

        result.setScore(score);
        result.setSubmitted(true);
        result.setAnswers(resultDTO.getAnswers());
        result.setSubmittedAt(now);
        resultRepository.save(result);

        notificationService.notifyExamSubmission(student, exam, score);

        return resultMapper.toSubmissionDTO(result, "Exam submitted successfully");
    }

    @Override
    public List<ResultDTO> getMyResults(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return resultRepository.findByStudentId(student.getId()).stream()
                .map(result -> resultMapper.toDTO(
                        result,
                        examRepository.findById(result.getExamId())
                                .map(Exam::getTitle)
                                .orElse("Unknown Exam")
                ))
                .toList();
    }

    @Override
    public long getRemainingSeconds(String examId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Result result = resultRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam registration not found"));

        if (result.isSubmitted()) {
            return 0L;
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BadRequestException("Exam has not started yet");
        }
        if (!exam.isActive()) {
            throw new BadRequestException("Exam is not active");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BadRequestException("Exam window is closed");
        }

        if (result.getAttemptStartAt() == null || result.getAttemptEndAt() == null) {
            LocalDateTime attemptStart = now;
            LocalDateTime byDuration = attemptStart.plusMinutes(exam.getDurationMinutes());
            LocalDateTime attemptEnd = exam.getEndTime() == null || byDuration.isBefore(exam.getEndTime())
                    ? byDuration
                    : exam.getEndTime();

            if (!attemptEnd.isAfter(attemptStart)) {
                throw new BadRequestException("Exam duration is over");
            }

            result.setAttemptStartAt(attemptStart);
            result.setAttemptEndAt(attemptEnd);
            resultRepository.save(result);
        }

        LocalDateTime hardEndTime = exam.getEndTime() == null
                ? result.getAttemptEndAt()
                : result.getAttemptEndAt().isBefore(exam.getEndTime()) ? result.getAttemptEndAt() : exam.getEndTime();
        return Math.max(0L, Duration.between(now, hardEndTime).getSeconds());
    }
}
