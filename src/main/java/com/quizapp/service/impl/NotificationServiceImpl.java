package com.quizapp.service.impl;

import com.quizapp.entity.Exam;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.service.EmailService;
import com.quizapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;

    @Value("${app.name:QuizApp}")
    private String appName;

    @Override
    public void notifyRegistration(User user) {
        String subject;
        String body;

        if (user.getRole() == Role.MENTOR) {
            subject = appName + " Mentor Registration Received";
            body = "Hi " + user.getName() + ",\n\n"
                    + "Your mentor registration is successful and pending admin approval.\n"
                    + "We will notify you as soon as your account is approved.\n\n"
                    + "Regards,\n" + appName;
        } else {
            subject = appName + " Registration Successful";
            body = "Hi " + user.getName() + ",\n\n"
                    + "Your account is created successfully.\n"
                    + "You can now login from the application.\n\n"
                    + "Regards,\n" + appName;
        }

        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    @Override
    public void notifyMentorApproved(User mentor) {
        String subject = appName + " Mentor Account Approved";
        String body = "Hi " + mentor.getName() + ",\n\n"
                + "Your mentor account is approved.\n"
                + "You can now login and create quizzes.\n"
                + "\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(mentor.getEmail(), subject, body);
    }

    @Override
    public void notifyExamRegistration(User student, Exam exam) {
        String subject = appName + " Exam Registration Confirmed";
        String body = "Hi " + student.getName() + ",\n\n"
                + "You are registered for exam: " + exam.getTitle() + ".\n"
                + "Start Time: " + exam.getStartTime() + "\n"
                + "End Time: " + exam.getEndTime() + "\n"
                + "Duration (minutes): " + exam.getDurationMinutes() + "\n\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(student.getEmail(), subject, body);
    }

    @Override
    public void notifyExamSubmission(User student, Exam exam, int score) {
        String subject = appName + " Exam Submitted";
        String body = "Hi " + student.getName() + ",\n\n"
                + "Your exam has been submitted successfully.\n"
                + "Exam: " + exam.getTitle() + "\n"
                + "Score: " + score + "\n\n"
                + "You can view results on your dashboard.\n\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(student.getEmail(), subject, body);
    }

    @Override
    public void notifyExamReminder(User student, Exam exam, String reminderLabel) {
        String subject = appName + " Exam Reminder: " + reminderLabel;
        String body = "Hi " + student.getName() + ",\n\n"
                + "Reminder: your registered exam is coming up.\n"
                + "Exam: " + exam.getTitle() + "\n"
                + "Start Time: " + exam.getStartTime() + "\n"
                + "End Time: " + exam.getEndTime() + "\n"
                + "Duration (minutes): " + exam.getDurationMinutes() + "\n"
                + "\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(student.getEmail(), subject, body);
    }

    @Override
    public void notifyRegistrationOtp(User user, String otpCode) {
        String subject = appName + " Email Verification OTP";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Use this OTP to verify your email: " + otpCode + "\n"
                + "OTP validity: 10 minutes\n\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    @Override
    public void notifyEmailVerified(User user) {
        String subject = appName + " Registration Successful";
        String roleNote = user.getRole() == Role.MENTOR
                ? "Your mentor account is pending admin approval.\n"
                : "Your account is active and ready to use.\n";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Your email has been verified successfully.\n"
                + roleNote
                + "\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    @Override
    public void notifyPasswordResetOtp(User user, String otpCode) {
        String subject = appName + " Password Reset OTP";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Use this OTP to reset your password: " + otpCode + "\n"
                + "OTP validity: 10 minutes\n\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    @Override
    public void notifyPasswordResetSuccess(User user) {
        String subject = appName + " Password Reset Successful";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Your password has been reset successfully.\n"
                + "\n"
                + "Regards,\n" + appName;
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }
}
