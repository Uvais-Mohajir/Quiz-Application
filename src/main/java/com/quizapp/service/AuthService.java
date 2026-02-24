package com.quizapp.service;

import com.quizapp.dto.UserDTO;

public interface AuthService {

    void register(UserDTO userDTO);

    void verifyRegistrationOtp(String email, String otp);

    void resendRegistrationOtp(String email);

    void sendPasswordResetOtp(String email);

    void resetPasswordWithOtp(String email, String otp, String newPassword);
}
