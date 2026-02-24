package com.quizapp.controller;

import com.quizapp.dto.ApiResponse;
import com.quizapp.dto.UserDTO;
import com.quizapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users fetched successfully")
    })
    public ApiResponse<List<UserDTO>> getAllUsers() {
        return ApiResponse.success("Users fetched successfully", adminService.getAllUsers());
    }

    @PutMapping("/approve-mentor/{mentorId}")
    @Operation(summary = "Approve mentor account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mentor approved successfully")
    })
    public ApiResponse<Void> approveMentor(@Parameter(description = "Mentor user id") @PathVariable String mentorId) {
        adminService.approveMentor(mentorId);
        return ApiResponse.success("Mentor approved successfully");
    }

    @PutMapping("/block-user/{userId}")
    @Operation(summary = "Block a user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User blocked successfully")
    })
    public ApiResponse<Void> blockUser(@Parameter(description = "User id") @PathVariable String userId) {
        adminService.blockUser(userId);
        return ApiResponse.success("User blocked successfully");
    }

    @PutMapping("/unblock-user/{userId}")
    @Operation(summary = "Unblock a user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User unblocked successfully")
    })
    public ApiResponse<Void> unblockUser(@Parameter(description = "User id") @PathVariable String userId) {
        adminService.unblockUser(userId);
        return ApiResponse.success("User unblocked successfully");
    }
}
