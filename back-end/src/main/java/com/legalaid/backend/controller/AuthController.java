package com.legalaid.backend.controller;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.security.JwtUtils;
import com.legalaid.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final JavaMailSender mailSender;

    // Temporary storage for OTPs (In production, use Redis or Database with expiration)
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    // Password validation regex: 
    // At least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public AuthController(AuthenticationManager authenticationManager, AuthService authService,
                          PasswordEncoder encoder, JwtUtils jwtUtils, JavaMailSender mailSender) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.mailSender = mailSender;
    }

    // DTOs for Requests/Responses
    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
        public LoginRequest {
            if (email != null) email = email.trim();
        }
    }
    public record SignupRequest(@NotBlank String username, @NotBlank @Email String email, @NotBlank String password,
                         @NotBlank String fullName, String role, String referenceId,
                         // Lawyer specific fields
                         String barRegistrationNo, String specialization, Integer experienceYears,
                         String city, String bio, String language, String contactInfo,
                         // NGO specific fields
                         String ngoName, String registrationNo, String website, String description) {
        public SignupRequest {
            if (email != null) email = email.trim();
            if (username != null) username = username.trim();
        }
    }
    record JwtResponse(String accessToken, String refreshToken, String email, String role) {}
    record MessageResponse(String message) {}
    
    record ForgotPasswordRequest(@NotBlank @Email String email) {}
    record VerifyOtpRequest(@NotBlank @Email String email, @NotBlank String otp) {}
    record ResetPasswordRequest(@NotBlank @Email String email, @NotBlank String otp, @NotBlank String newPassword) {}
    record RefreshTokenRequest(@NotBlank String refreshToken) {}

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
            log.debug("Authentication successful for email: {}", loginRequest.email());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            org.springframework.security.core.userdetails.UserDetails userDetails =
                    (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            User user = authService.getUserByEmail(loginRequest.email());
            // Status check (only ACTIVE users can login)
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Login rejected - user status {} for email: {}",user.getStatus(),loginRequest.email());

            return ResponseEntity.status(401)
                .body(new MessageResponse(
                    "Your account is not active. Please contact support."
            ));
            }
            boolean isLawyerOrNgo = user.getRole() == Role.LAWYER || user.getRole() == Role.NGO;
            Boolean approved = user.getApproved();
            log.debug("User role: {}, Approved: {}", user.getRole(), approved);
            
            if (isLawyerOrNgo) {
                if (approved == null) {
                    log.warn("Login rejected - admin approval pending for email: {}", loginRequest.email());
                    return ResponseEntity.status(401)
                            .body(new MessageResponse("Admin hasn't approved your profile. Please wait for approval"));
                }
                if (Boolean.FALSE.equals(approved)) {
                    log.warn("Login rejected - profile rejected for email: {}", loginRequest.email());
                    return ResponseEntity.status(401)
                            .body(new MessageResponse("Your profile was rejected by the admin"));
                }
            }

            String accessToken = jwtUtils.generateAccessToken(userDetails);
            String refreshToken = jwtUtils.generateRefreshToken(userDetails);
            String role = user.getRole().name();
            
            log.info("Login successful for email: {} with role: {}", loginRequest.email(), role);
            return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, user.getEmail(), role));
        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.email(), e);
            return ResponseEntity.status(401).body(new MessageResponse("Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Registration attempt for email: {}, role: {}", signUpRequest.email(), signUpRequest.role());
        log.info("SignupRequest details - referenceId: {}, barRegistrationNo: {}, specialization: {}, experienceYears: {}, city: {}, bio: {}, language: {}, contactInfo: {}, ngoName: {}, registrationNo: {}, website: {}, description: {}",
                signUpRequest.referenceId(), signUpRequest.barRegistrationNo(), signUpRequest.specialization(),
                signUpRequest.experienceYears(), signUpRequest.city(), signUpRequest.bio(),
                signUpRequest.language(), signUpRequest.contactInfo(), signUpRequest.ngoName(),
                signUpRequest.registrationNo(), signUpRequest.website(), signUpRequest.description());

        if (authService.existsByEmail(signUpRequest.email())) {
            log.warn("Registration failed - email already exists: {}", signUpRequest.email());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }
        if (authService.existsByUsername(signUpRequest.username())) {
            log.warn("Registration failed - username already exists: {}", signUpRequest.username());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already in use!"));
        }

        // Validate password strength
        if (!isValidPassword(signUpRequest.password())) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Error: Password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character (@#$%^&+=!)."
            ));
        }

        // Determine Role ,Default to CITIZEN if invalid or null
        Role userRole;
        try {
            userRole = Role.valueOf(signUpRequest.role().toUpperCase());
        } catch (Exception e) {
            log.debug("Invalid role provided: {}, defaulting to CITIZEN", signUpRequest.role());
            userRole = Role.CITIZEN;
        }

        Boolean approved = (userRole == Role.LAWYER || userRole == Role.NGO) ? null : Boolean.TRUE; // admin approval needed for lawyer/ngo, set null until action
        UserStatus status = (userRole == Role.LAWYER || userRole == Role.NGO) ? UserStatus.PENDING : UserStatus.ACTIVE; // Default to active status

        log.debug("Creating user with username: {}, role: {}", signUpRequest.username(), userRole);
        User user = new User(
                signUpRequest.username(),
                signUpRequest.email(),
                encoder.encode(signUpRequest.password()),
                signUpRequest.fullName(),
                userRole,
                approved,
                status
        );

        try {
            authService.registerUserWithClaim(user, signUpRequest.referenceId(), signUpRequest);
            log.info("User registered successfully - email: {}, role: {}", signUpRequest.email(), userRole);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        log.info("Token refresh request received with token: {}", refreshToken);

        if (jwtUtils.validateToken(refreshToken)) {
            log.info("Refresh token is valid.");
            try {
                String username = jwtUtils.extractUsername(refreshToken);
                log.debug("Token refresh for username: {}", username);
                org.springframework.security.core.userdetails.UserDetails userDetails =
                        authService.loadUserByUsername(username);

                String newAccessToken = jwtUtils.generateAccessTokenFromUsername(username);
                String role = userDetails.getAuthorities().iterator().next().getAuthority();

                log.info("Token refreshed successfully for username: {}", username);
                return ResponseEntity.ok(new JwtResponse(newAccessToken, refreshToken, username, role));
            } catch (Exception e) {
                log.error("Token refresh failed during user processing", e);
                return ResponseEntity.status(401).body(new MessageResponse("Error processing refresh token."));
            }
        } else {
            log.warn("Token refresh failed - invalid or expired refresh token.");
            return ResponseEntity.status(401).body(new MessageResponse("Invalid or expired refresh token."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.email();
        log.info("Forgot password request for email: {}", email);

        if (!authService.existsByEmail(email)) {
            log.warn("Forgot password failed - email not found: {}", email);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is not registered!"));
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(email, otp);
        log.info("Generated OTP for email: {}", email);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Legal Aid Platform - Password Reset OTP");
            message.setText("Your OTP for password reset is: " + otp + "\n\nThis OTP is valid for a short time.");
            mailSender.send(message);
            log.info("OTP sent successfully to email: {}", email);
            return ResponseEntity.ok(new MessageResponse("OTP sent to your email!"));
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error sending email. Please try again later."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = request.email();
        String otp = request.otp();
        
        log.info("OTP verification attempt for email: {}", email);

        if (!otpStorage.containsKey(email) || !otpStorage.get(email).equals(otp)) {
            log.warn("OTP verification failed - invalid or expired OTP for email: {}", email);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired OTP!"));
        }

        return ResponseEntity.ok(new MessageResponse("OTP verified successfully!"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String email = request.email();
        String otp = request.otp();
        String newPassword = request.newPassword();
        
        log.info("Reset password attempt for email: {}", email);

        if (!otpStorage.containsKey(email) || !otpStorage.get(email).equals(otp)) {
            log.warn("Reset password failed - invalid or expired OTP for email: {}", email);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired OTP!"));
        }

        // Validate new password strength
        if (!isValidPassword(newPassword)) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Error: Password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character (@#$%^&+=!)."
            ));
        }

        try {
            User user = authService.getUserByEmail(email);
            user.setPassword(encoder.encode(newPassword));
            authService.saveUser(user);
            otpStorage.remove(email); // Clear OTP after successful reset
            log.info("Password reset successfully for email: {}", email);
            return ResponseEntity.ok(new MessageResponse("Password reset successfully! You can now login."));
        } catch (Exception e) {
            log.error("Failed to reset password for email: {}", email, e);
            return ResponseEntity.internalServerError().body(new MessageResponse("Error resetting password."));
        }
    }

    private boolean isValidPassword(String password) {
        return password != null && pattern.matcher(password).matches();
    }
}
