package com.quizapp.controller;

import com.quizapp.dto.ApiResponse;
import com.quizapp.dto.LoginDTO;
import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.UnauthorizedException;
import com.quizapp.mapper.UserMapper;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and login APIs for participant, mentor, and admin")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/user/register")
    @Operation(summary = "Register participant")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponse<Void>> userRegister(@Valid @RequestBody UserDTO userDTO) {
        userDTO.setRole(Role.PARTICIPANT);
        authService.register(userDTO);
        return ResponseEntity.ok(new ApiResponse(true, "Registration successful", null));
    }

    @PostMapping("/mentor/register")
    @Operation(summary = "Register mentor")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mentor registration successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ApiResponse<Void>> mentorRegister(@Valid @RequestBody UserDTO userDTO) {
        userDTO.setRole(Role.MENTOR);
        authService.register(userDTO);
        return ResponseEntity.ok(new ApiResponse(true, "Mentor registration successful", null));
    }

    @PostMapping("/user/login")
    @Operation(summary = "Participant login")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(hidden = true)))
    })
    public ApiResponse<UserDTO> userLogin(@Valid @RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        return loginByRole(request, Role.PARTICIPANT, httpRequest);
    }

    @PostMapping("/mentor/login")
    @Operation(summary = "Mentor login")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(hidden = true)))
    })
    public ApiResponse<UserDTO> mentorLogin(@Valid @RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        return loginByRole(request, Role.MENTOR, httpRequest);
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Admin login")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(hidden = true)))
    })
    public ApiResponse<UserDTO> adminLogin(@Valid @RequestBody LoginDTO request, HttpServletRequest httpRequest) {
        return loginByRole(request, Role.ADMIN, httpRequest);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify registration OTP")
    public ApiResponse<Void> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        authService.verifyRegistrationOtp(email, otp);
        return ApiResponse.success("Email verified successfully");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend registration OTP")
    public ApiResponse<Void> resendOtp(@RequestParam String email) {
        authService.resendRegistrationOtp(email);
        return ApiResponse.success("OTP sent successfully");
    }

    @PostMapping("/forgot-password/send-otp")
    @Operation(summary = "Send password reset OTP")
    public ApiResponse<Void> sendForgotPasswordOtp(@RequestParam String email) {
        authService.sendPasswordResetOtp(email);
        return ApiResponse.success("Password reset OTP sent successfully");
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password using OTP")
    public ApiResponse<Void> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword
    ) {
        authService.resetPasswordWithOtp(email, otp, newPassword);
        return ApiResponse.success("Password reset successful");
    }

    private ApiResponse<UserDTO> loginByRole(LoginDTO request, Role role, HttpServletRequest httpRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            throw new UnauthorizedException("Account is not approved or email is not verified");
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getRole() != role) {
            throw new UnauthorizedException("Invalid login endpoint for this role");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        UserDTO userDTO = userMapper.toDTO(user);
        userDTO.setToken(session.getId());
        return ApiResponse.success("Login successful", userDTO);
    }
}
