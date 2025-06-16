package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ChangePasswordRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.UserDto;
import com.instagram.backend.dtos.response.UserProfileDto;
import com.instagram.backend.dtos.response.UserSearchResponse;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.enums.ConnectionType;
import com.instagram.backend.exception.BadCredentialsException;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.PostRepository;
import com.instagram.backend.repository.UserConnectionRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthenticationFacade authenticationFacade;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserConnectionRepository userConnectionRepository;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;


    /**
     * Allows the logged-in user to change their password after verifying the old one.
     */
    @Override
    public ApiResponse<String> changePassword(ChangePasswordRequest request) {
        log.info("Change password request received.");

        User user = getLoggedInUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Old password mismatch for user: {}", user.getUsername());
            throw new BadCredentialsException("Old password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password updated for user: {}", user.getUsername());

        sendPasswordChangeEmail(user);
        return ApiResponse.success("Password changed successfully");
    }


    /**
     * Returns the profile details of the currently authenticated user.
     */
    @Override
    public ApiResponse<UserProfileDto> getMyProfile() {
        log.info("Fetching profile for logged-in user.");
        return ApiResponse.success(toProfileDto(getLoggedInUser()), "Your profile fetched successfully.");
    }


    /**
     * Returns the profile details of another user, considering privacy settings.
     */
    @Override
    public ApiResponse<UserProfileDto> getUserProfile(Long userId) {
        log.info("Fetching profile for userId={}", userId);
        User targetUser = validateToUser(userId);
        validateProfileVisibility(targetUser, getLoggedInUser());
        return ApiResponse.success(toProfileDto(targetUser), "User profile fetched successfully.");
    }


    /**
     * Searches users by username. Respects private profile settings.
     */
    @Override
    public ApiResponse<UserSearchResponse> searchUsersByUsername(String username) {
        log.info("Searching users by username containing: {}", username);
        User currentUser = getLoggedInUser();

        List<UserDto> userDtoList = userRepository.findByUsernameContainingIgnoreCase(username).stream()
                .filter(user -> !user.isPrivateProfile() || user.getId().equals(currentUser.getId()))
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .profileImageUrl(generateProfileImageUrl(user.getUsername()))
                        .build())
                .toList();

        UserSearchResponse response = UserSearchResponse.builder()
                        .users(userDtoList)
                        .count(userDtoList.size())
                        .build();

        log.info("Found {} users matching search", userDtoList.size());

        return ApiResponse.success(response, "Search results");
    }


    /**
     * Allows the logged-in user to toggle profile privacy.
     */
    @Override
    public ApiResponse<String> togglePrivacy(boolean privateProfile) {
        User user = getLoggedInUser();
        user.setPrivateProfile(privateProfile);
        userRepository.save(user);
        log.info("Privacy setting changed to {} for user {}", privateProfile, user.getUsername());
        return ApiResponse.success("Privacy setting updated successfully.");
    }


    /**
     * Checks if a user has a private profile.
     */
    @Override
    public boolean isPrivateProfile(Long userId) {
        return validateToUser(userId).isPrivateProfile();
    }


    /**
     * Fetches a user by ID.
     */
    @Override
    public ApiResponse<UserDto> getUserById(Long userId) {
        User user = validateToUser(userId);
        log.info("User fetched by ID: {}", userId);
        return ApiResponse.success(modelMapper.map(user, UserDto.class), "User fetched successfully");
    }


    // ----------------- PRIVATE METHODS -------------------

    /**
     * Returns the currently authenticated user from the security context.
     */
    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }


    /**
     * Validates and retrieves a user by ID, must be enabled.
     */
    private User validateToUser(Long toUserId) {
        return userRepository.findById(toUserId)
                .filter(User::isEnabled)
                .orElseThrow(() -> {
                    log.warn("User not found or disabled: id={}", toUserId);
                    return new UserNotFoundException("User not found or not enabled with id: " + toUserId);
                });
    }


    /**
     * Converts User entity to UserProfileDto with follower/following/post counts.
     */
    private UserProfileDto toProfileDto(User user) {
        int followers = userConnectionRepository.countByToUserAndConnectionType(user, ConnectionType.FOLLOW);
        int following = userConnectionRepository.countByFromUserAndConnectionType(user, ConnectionType.FOLLOW);
        int posts = postRepository.countByUser(user);

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName("Instagram User")
                .email(user.getEmail())
                .bio("Hi, I am Instagram User. I'm exploring Instagram!")
                .profileImageUrl(generateProfileImageUrl(user.getUsername()))
                .postsCount(posts)
                .followersCount(followers)
                .followingCount(following)
                .build();
    }


    /**
     * Generates a default profile image URL based on username.
     */
    private String generateProfileImageUrl(String username) {
        return "https://ui-avatars.com/api/?name=" + username + "&background=random&rounded=true";
    }


    /**
     * Validates that a private profile is accessible only by the owner.
     */
    public void validateProfileVisibility(User targetUser, User currentUser) {
        if (targetUser.isPrivateProfile() && !targetUser.getId().equals(currentUser.getId())) {
            log.warn("Access denied to private profile: userId={}", targetUser.getId());
            throw new AccessDeniedException("This profile is private.");
        }
    }


    /**
     * Sends an email to the user after a successful password change.
     */
    private void sendPasswordChangeEmail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());

        emailService.sendEmail(
                user.getEmail(),
                "Password Changed Successfully",
                "password-changed",
                variables
        );
        log.info("Password change email sent to {}", user.getEmail());
    }

}
