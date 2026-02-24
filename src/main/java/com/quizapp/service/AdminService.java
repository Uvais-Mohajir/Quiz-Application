package com.quizapp.service;

import com.quizapp.dto.UserDTO;
import java.util.List;

public interface AdminService {

    List<UserDTO> getAllUsers();

    void approveMentor(String mentorId);

    void blockUser(String userId);

    void unblockUser(String userId);
}
