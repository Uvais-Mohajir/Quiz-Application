package com.quizapp.mapper;

import com.quizapp.dto.UserDTO;
import com.quizapp.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        return User.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .role(dto.getRole())
                .approved(dto.isApproved())
                .emailVerified(dto.isEmailVerified())
                .build();
    }

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setApproved(user.isApproved());
        dto.setEmailVerified(user.isEmailVerified());
        return dto;
    }
}
