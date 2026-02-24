package com.quizapp.service.impl;

import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import com.quizapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    public void register(UserDTO userDTO) {

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = userMapper.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setApproved(userDTO.getRole() != Role.MENTOR);
        user.setEmailVerified(false);
        user.setEmailOtp(generateOtp());
        user.setEmailOtpExpiry(LocalDateTime.now().plusMinutes(10));

        User savedUser = userRepository.save(user);
        try {
            notificationService.notifyRegistrationOtp(savedUser, savedUser.getEmailOtp());
        } catch (RuntimeException ex) {
            // Best-effort rollback so failed OTP dispatch does not leave a persisted account behind.
            userRepository.deleteById(savedUser.getId());
            throw ex;
        }
    }

    @Override
    public void verifyRegistrationOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }
        if (user.getEmailOtp() == null || user.getEmailOtpExpiry() == null) {
            throw new BadRequestException("OTP not generated. Please resend OTP");
        }
        if (LocalDateTime.now().isAfter(user.getEmailOtpExpiry())) {
            throw new BadRequestException("OTP expired. Please resend OTP");
        }
        if (!user.getEmailOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setEmailOtp(null);
        user.setEmailOtpExpiry(null);
        userRepository.save(user);
        notificationService.notifyEmailVerified(user);
    }

    @Override
    public void resendRegistrationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }

        String otp = generateOtp();
        user.setEmailOtp(otp);
        user.setEmailOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        notificationService.notifyRegistrationOtp(user, otp);
    }

    @Override
    public void sendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Email is not verified");
        }

        String otp = generateOtp();
        user.setEmailOtp(otp);
        user.setEmailOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        notificationService.notifyPasswordResetOtp(user, otp);
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailOtp() == null || user.getEmailOtpExpiry() == null) {
            throw new BadRequestException("OTP not generated. Request password reset OTP");
        }
        if (LocalDateTime.now().isAfter(user.getEmailOtpExpiry())) {
            throw new BadRequestException("OTP expired. Request password reset OTP again");
        }
        if (!user.getEmailOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEmailOtp(null);
        user.setEmailOtpExpiry(null);
        userRepository.save(user);
        notificationService.notifyPasswordResetSuccess(user);
    }

    private String generateOtp() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return Integer.toString(value);
    }
}
