package com.quizapp.service.impl;

import com.quizapp.dto.ExamDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.mapper.ExamMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ExamMapper examMapper;

    @Override
    public ExamDTO createExam(ExamDTO examDTO, String creatorEmail) {

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!creator.isApproved()) {
            throw new BadRequestException("Account is blocked");
        }
        if (examDTO.getDurationMinutes() <= 0) {
            throw new BadRequestException("Duration must be greater than zero");
        }
        if (examDTO.getStartTime() == null || examDTO.getEndTime() == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (!examDTO.getEndTime().isAfter(examDTO.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Exam exam = examMapper.toEntity(examDTO, creator.getId());

        examRepository.save(exam);

        return examMapper.toDTO(exam);
    }

    @Override
    public List<ExamDTO> getActiveExams() {

        LocalDateTime now = LocalDateTime.now();

        return examRepository.findByActiveTrue()
                .stream()
                .filter(exam -> exam.getEndTime() == null || exam.getEndTime().isAfter(now))
                .sorted((a, b) -> {
                    if (a.getStartTime() == null && b.getStartTime() == null) {
                        return 0;
                    }
                    if (a.getStartTime() == null) {
                        return 1;
                    }
                    if (b.getStartTime() == null) {
                        return -1;
                    }
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .filter(exam -> questionRepository.findByExamId(exam.getId()).size() >= 3)
                .map(examMapper::toDTO)
                .toList();
    }

    @Override
    public List<ExamDTO> getExamsByMentor(String mentorEmail) {
        User mentor = userRepository.findByEmail(mentorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor not found"));

        if (mentor.getRole() != Role.MENTOR) {
            throw new BadRequestException("Only mentors can view mentor exams");
        }

        return examRepository.findByMentorIdOrderByStartTimeDesc(mentor.getId())
                .stream()
                .map(examMapper::toDTO)
                .toList();
    }

    @Override
    public ExamDTO getMentorExamById(String examId, String mentorEmail) {
        User mentor = userRepository.findByEmail(mentorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (!mentor.getId().equals(exam.getMentorId())) {
            throw new BadRequestException("You can only access your own exams");
        }

        return examMapper.toDTO(exam);
    }
}
