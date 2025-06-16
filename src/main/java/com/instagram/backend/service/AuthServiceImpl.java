package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ForgotPasswordRequest;
import com.instagram.backend.dtos.request.LoginRequest;
import com.instagram.backend.dtos.request.RegisterRequest;
import com.instagram.backend.dtos.request.ResetPasswordRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.LoginResponse;
import com.instagram.backend.dtos.response.RegistrationResponse;
import com.instagram.backend.entity.PasswordResetToken;
import com.instagram.backend.entity.Role;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.VerificationToken;
import com.instagram.backend.exception.*;
import com.instagram.backend.repository.PasswordResetTokenRepository;
import com.instagram.backend.repository.RoleRepository;
import com.instagram.backend.repository.UserRepository;
import com.instagram.backend.repository.VerificationTokenRepository;
import com.instagram.backend.security.CustomUserDetailsService;
import com.instagram.backend.security.JwtService;
import com.instagram.backend.utils.OtpSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "USER";
    private static final long VERIFICATION_TOKEN_EXPIRY_MINUTES = 60;
    private static final long RESET_TOKEN_EXPIRY_MINUTES = 15;


    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpSessionStore otpSessionStore;


    @Override
    public ApiResponse<RegistrationResponse> registerUser(RegisterRequest request, String appUrl) {
        log.info("Registering user: {}", request.getEmail());

        validateEmailAndUsername(request.getEmail(), request.getUsername());


        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setProfileImageUrl(generateProfileImageUrl(request.getUsername()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        // Utility call to fetch roles from DB
        user.setRoles(fetchRoles(Set.of(DEFAULT_ROLE)));

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());

        // Send verification token
        sendEmailVerificationEmail(user, appUrl);

        RegistrationResponse response = mapper.map(savedUser, RegistrationResponse.class);

        return ApiResponse.success(response, "User registered successfully");
    }

    @Override
    public ApiResponse<String> verifyUser(String token) {
        log.info("Starting verification for token: {}", token);

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserNotFoundException("Invalid verification token"));


        validateTokenExpiry(verificationToken.getExpiryDate());

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        sendWelcomeEmail(user);

        log.info("User verified successfully: {}", user.getUsername());
        return ApiResponse.success("User verified successfully");
    }

//    @Override
//    public ApiResponse<LoginResponse> login(LoginRequest request) {
//        log.info("Login attempt for: {}", request.getUsername());
//
//        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
//                .orElseThrow(() -> new UserNotFoundException("Invalid username/email"));
//
//        if (!user.isEnabled()) {
//            log.warn("User tried to login but account is not enabled: {}", user.getUsername());
//            throw new EmailNotVerifiedException("Account is not verified yet. Please check your email.");
//        }
//
//        // 3. Manually verify password before authentication
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new BadCredentialsException("Invalid password");
//        }
//
//        // If password is wrong, this will throw BadCredentialsException
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
//        );
//
//        String jwtToken = jwtService.generateToken(
//                userDetailsService.loadUserByUsername(user.getUsername()),
//                user.getId()
//        );
//
//        String refreshToken = jwtService.generateRefreshToken(userDetailsService.loadUserByUsername(user.getUsername()));
//
//        log.info("JWT token generated successfully for user: {}", jwtToken);
//        log.info("User logged in successfully: {}", user.getUsername());
//
//        LoginResponse response = LoginResponse.builder()
//                .jwtToken(jwtToken)
//                .refreshToken(refreshToken)
//                .username(request.getUsername())
//                .build();
//
//        return ApiResponse.success(response, "Login successful");
//    }

    @Override
    public ApiResponse<String> login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email"));

        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Account is not verified yet. Please check your email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        generateAndSendOTP(user);
        log.info("OTP sent to user email: {}", user.getEmail());
        // ✅ Create and store tempToken → map to user
        String tempToken = UUID.randomUUID().toString();
        otpSessionStore.save(tempToken, user.getUsername());


        return ApiResponse.success(tempToken, "OTP sent to your email. Please verify to continue.");
    }

    @Override
    public ApiResponse<LoginResponse> verifyOtp(String tempToken, String enteredOtp) {
        // 1. Validate tempToken exists
        if (!otpSessionStore.exists(tempToken)) {
            throw new TokenExpiredException("Invalid or expired session token.");
        }

        String username = otpSessionStore.getUsername(tempToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        // 4. Check OTP and expiry
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new BadCredentialsException("OTP not generated. Please login again.");
        }

        if (!enteredOtp.equals(user.getOtp()) || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        // 5. Clear OTP after verification
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // 6. Generate JWT tokens
        String jwtToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(user.getUsername()),
                user.getId()
        );
        String refreshToken = jwtService.generateRefreshToken(userDetailsService.loadUserByUsername(user.getUsername()));

        // 7. Clean up temp session
        otpSessionStore.remove(tempToken);

        log.info("JWT token generated successfully for user: {}", jwtToken);
        log.info("User logged in successfully: {}", user.getUsername());

        LoginResponse response = LoginResponse.builder()
                .jwtToken(jwtToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .build();

        return ApiResponse.success(response, "Login successful");
    }

    @Override
    public ApiResponse<String> resendOtp(String tempToken) {
        if (!otpSessionStore.exists(tempToken)) {
            throw new TokenExpiredException("Invalid or expired session token.");
        }

        String username = otpSessionStore.getUsername(tempToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        generateAndSendOTP(user);
        return ApiResponse.success("OTP resent successfully to " + user.getEmail(), "OTP resent");
    }


    @Override
    public ApiResponse<LoginResponse> refreshToken(String refreshToken) {
        log.info("Attempting to refresh access token using refresh token...");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            log.warn("Refresh token is invalid or expired for user: {}", username);
            throw new TokenExpiredException("Expired refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username : " + username));

        // Generate new tokens (rotate refresh token)
        String newAccessToken = jwtService.generateToken(userDetails, user.getId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        LoginResponse responseBody = LoginResponse.builder()
                .jwtToken(newAccessToken)
                .refreshToken(newRefreshToken) // do NOT expose refresh token in body
                .username(username)
                .build();

        log.info("Access token refreshed successfully for user: {}", username);
        return ApiResponse.success(responseBody, "Access token refreshed");
    }

    @Override
    @Transactional
    public ApiResponse<String> resendTokenByEmail(String email, String appUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with this email"));

        if (user.isEnabled()) {
            log.info("User '{}' already verified, skipping resend token", user.getUsername());
            throw new AccountAlreadyVerifiedException("Account is already verified");
        }

        // Delete existing tokens first to avoid duplicates
        verificationTokenRepository.deleteByUserId(user.getId());
        verificationTokenRepository.flush();

        sendEmailVerificationEmail(user, appUrl);

        log.info("New verification token sent to user '{}'", user.getUsername());
        return ApiResponse.success("New verification email has been sent");
    }

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request, String appUrl) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No user associated with this email"));

        String token = generateRandomToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));

        passwordResetTokenRepository.save(resetToken);
        String resetUrl = appUrl + "/api/v1/auth/reset-password?token=" + token;

        sendEmail(user, "Reset Your Password", "forgot-password-email", Map.of(
                "username", user.getUsername(),
                "resetUrl", resetUrl
        ));
        log.info("Password reset link sent to {}", user.getEmail());

        return ApiResponse.success("Password reset link sent to your email");
    }

    @Override
    public ApiResponse<String> resetPassword(ResetPasswordRequest request, String token) {
        PasswordResetToken resetToken  = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        validateTokenExpiry(resetToken .getExpiryDate());


        User user = resetToken .getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken );

        sendEmail(user, "Password Reset Successful", "password-reset-success-email", Map.of(
                "username", user.getUsername()
        ));

        log.info("Password reset successful for user: {}", user.getUsername());

        return ApiResponse.success("Password reset successfully");
    }


    // ----------------- PRIVATE UTILITY METHODS -------------------


    private Set<Role> fetchRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
    }

    private void validateEmailAndUsername(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("Email already in use");
        }
        if (userRepository.existsByUsername(username)) {
            throw new EmailAlreadyUsedException("Username already taken");
        }
    }

    private void sendWelcomeEmail(User user) {
        sendEmail(user, "Welcome to InstaClone!", "welcome-email", Map.of(
                "username", user.getUsername()
        ));
    }

    private void sendEmail(User user, String subject, String template, Map<String, Object> variables) {
        emailService.sendEmail(user.getEmail(), subject, template, variables);
        log.info("Email '{}' sent to {}", subject, user.getEmail());
    }

    private String generateRandomToken() {
        return UUID.randomUUID().toString();
    }

    private void sendEmailVerificationEmail(User user, String appUrl) {
        String token = generateRandomToken();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_EXPIRY_MINUTES));

        VerificationToken savedToken = verificationTokenRepository.save(verificationToken);

        String verifyUrl = appUrl + "/api/v1/auth/verify-email?token=" + token;

        sendEmail(user, "Verify Your Email", "verify-email", Map.of(
                "username", user.getUsername(),
                "verifyUrl", verifyUrl
        ));

        log.debug("Verification URL sent: {}", verifyUrl);

        log.info("Verification email sent to {}", user.getEmail());
    }

    private void validateTokenExpiry(LocalDateTime expiryDate) {
        if (expiryDate.isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Verification token has expired");
        }
    }

    public void generateAndSendOTP(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        Instant expiry = Instant.now().plus(Duration.ofMinutes(5));

        user.setOtp(otp);
        user.setOtpExpiry(expiry);
        userRepository.save(user);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("otp", otp);

        // Email logic (You can use JavaMailSender or Spring Email service)
        String subject = "Your OTP for Login";

        emailService.sendEmail(user.getEmail(), subject, "otp-verification", variables);
    }

    private String generateProfileImageUrl(String username) {
        // Use a dummy image avatar service (fallback to initials)
        return "https://ui-avatars.com/api/?name=" + username + "&background=random&rounded=true";
    }

}
