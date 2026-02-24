package com.quizapp.service.impl;

import com.quizapp.entity.Exam;
import com.quizapp.entity.Result;
import com.quizapp.entity.User;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.scheduler.exam-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class ExamReminderScheduler {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60000)
    public void sendUpcomingExamReminders() {
        LocalDateTime now = LocalDateTime.now();

        for (Result registration : resultRepository.findBySubmittedFalse()) {
            if (registration.getExamId() == null || registration.getStudentId() == null) {
                continue;
            }

            Optional<Exam> examOpt = examRepository.findById(registration.getExamId());
            Optional<User> studentOpt = userRepository.findById(registration.getStudentId());
            if (examOpt.isEmpty() || studentOpt.isEmpty()) {
                continue;
            }

            Exam exam = examOpt.get();
            User student = studentOpt.get();

            if (exam.getStartTime() == null || now.isAfter(exam.getStartTime())) {
                continue;
            }

            long secondsToStart = Duration.between(now, exam.getStartTime()).getSeconds();
            boolean updated = false;

            if (!registration.isDayReminderSent()
                    && secondsToStart <= 24 * 3600L
                    && secondsToStart > 0L) {
                notificationService.notifyExamReminder(student, exam, "Starts within 24 hours");
                registration.setDayReminderSent(true);
                updated = true;
            }

            if (!registration.isHourReminderSent()
                    && secondsToStart <= 3600L
                    && secondsToStart > 0L) {
                notificationService.notifyExamReminder(student, exam, "Starts within 1 hour");
                registration.setHourReminderSent(true);
                updated = true;
            }

            if (updated) {
                resultRepository.save(registration);
            }
        }
    }
}
