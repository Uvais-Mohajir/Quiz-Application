package com.quizapp.service.impl;

import com.quizapp.dto.UserDTO;
import com.quizapp.entity.*;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AdminService;
import com.quizapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Override
    public void approveMentor(String mentorId) {
        User user = userRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.MENTOR) {
            throw new BadRequestException("Only mentor accounts can be approved");
        }

        user.setApproved(true);
        userRepository.save(user);
        notificationService.notifyMentorApproved(user);
    }

    @Override
    public void blockUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setApproved(false);
        userRepository.save(user);
    }

    @Override
    public void unblockUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setApproved(true);
        userRepository.save(user);
    }
}
