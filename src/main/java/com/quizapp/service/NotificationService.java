package com.quizapp.service;

import com.quizapp.entity.Exam;
import com.quizapp.entity.User;

public interface NotificationService {

    void notifyRegistration(User user);

    void notifyMentorApproved(User mentor);

    void notifyExamRegistration(User student, Exam exam);

    void notifyExamSubmission(User student, Exam exam, int score);

    void notifyExamReminder(User student, Exam exam, String reminderLabel);

    void notifyRegistrationOtp(User user, String otpCode);

    void notifyEmailVerified(User user);

    void notifyPasswordResetOtp(User user, String otpCode);

    void notifyPasswordResetSuccess(User user);
}
