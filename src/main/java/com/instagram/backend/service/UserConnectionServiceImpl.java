package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ConnectionRequestDto;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.ConnectionResponseDto;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.UserConnection;
import com.instagram.backend.entity.enums.ConnectionType;
import com.instagram.backend.entity.enums.NotificationType;
import com.instagram.backend.exception.*;
import com.instagram.backend.repository.UserConnectionRepository;
import com.instagram.backend.repository.UserRepository;
import com.instagram.backend.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserConnectionServiceImpl implements UserConnectionService{

    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final AuthenticationFacade authenticationFacade;
    private final NotificationService notificationService;

    /**
     * Allows the logged-in user to follow another user.
     */
    @Override
    public ApiResponse<String> followUser(ConnectionRequestDto dto) {
        User fromUser = getLoggedInUser();
        User toUser = validateToUser(dto.getToUserId());

        assertNotSelf(fromUser, toUser, "follow");

        // 🔍 Check if current user already has a connection to the target
        getConnection(fromUser, toUser).ifPresent(conn -> {
            ConnectionType type = conn.getConnectionType();
            if (type == ConnectionType.FOLLOW) {
                log.info("User {} already follows user {}", fromUser.getId(), toUser.getId());
                throw new AlreadyFollowedException("You already follow this user.");
            } else if (type == ConnectionType.BLOCK) {
                log.warn("User {} blocked user {}. Cannot follow.", fromUser.getId(), toUser.getId());
                throw new InvalidConnectionStateException("You have blocked this user. Unblock them first.");
            }
            throw new InvalidConnectionStateException("Existing connection of type: " + type.name());
        });


        if (getConnection(toUser, fromUser).filter(c -> c.getConnectionType() == ConnectionType.FOLLOW).isPresent()) {
            log.info("Follow back needed from {} to {}", fromUser.getId(), toUser.getId());
            throw new InvalidConnectionStateException("This user already follows you. Use follow back.");
        }

        saveConnection(fromUser, toUser, ConnectionType.FOLLOW);
        log.info("User {} followed user {}", fromUser.getId(), toUser.getId());
        notificationService.sendNotification(fromUser, toUser, NotificationType.FOLLOW);
        return ApiResponse.success("Followed successfully.");
    }


    /**
     * Allows the logged-in user to follow back a user who already follows them.
     */
    @Override
    public ApiResponse<String> followBackUser(ConnectionRequestDto dto) {
        User fromUser = getLoggedInUser(); // current user
        User toUser = validateToUser(dto.getToUserId()); // the user who already follows current user

        assertNotSelf(fromUser, toUser, "follow back");

        if (getConnection(toUser, fromUser).filter(c -> c.getConnectionType() == ConnectionType.FOLLOW).isEmpty()) {
            log.warn("User {} not followed by {}. Cannot follow back.", fromUser.getId(), toUser.getId());
            throw new InvalidConnectionStateException("Only possible to follow back users who follow you.");
        }

        getConnection(fromUser, toUser).ifPresent(conn -> {
            ConnectionType type = conn.getConnectionType();
            if (type == ConnectionType.FOLLOW) {
                throw new AlreadyFollowedException("You already followed back this user.");
            } else if (type == ConnectionType.BLOCK) {
                throw new InvalidConnectionStateException("You have blocked this user. Unblock to follow back.");
            }
            throw new InvalidConnectionStateException("Existing connection of type: " + type.name());
        });

        saveConnection(fromUser, toUser, ConnectionType.FOLLOW);
        log.info("User {} followed back user {}", fromUser.getId(), toUser.getId());
        notificationService.sendNotification(fromUser, toUser, NotificationType.FOLLOW_BACK);
        return ApiResponse.success("Followed back successfully.");
    }


    /**
     * Allows the logged-in user to unfollow another user.
     */
    @Override
    public ApiResponse<String> unfollowUser(ConnectionRequestDto dto) {
        User fromUser = getLoggedInUser();
        User toUser = validateToUser(dto.getToUserId());
        assertNotSelf(fromUser, toUser, "unfollow");

        UserConnection connection = getConnection(fromUser, toUser)
                .filter(c -> c.getConnectionType() == ConnectionType.FOLLOW)
                .orElseThrow(() -> new NotFollowingException("You are not following this user."));

        userConnectionRepository.delete(connection);
        log.info("User {} unfollowed user {}", fromUser.getId(), toUser.getId());
        notificationService.sendNotification(fromUser, toUser, NotificationType.UNFOLLOW);
        return ApiResponse.success("Unfollowed successfully.");
    }

    /**
     * Allows the logged-in user to block another user.
     */
    @Override
    public ApiResponse<String> blockUser(ConnectionRequestDto dto) {
        User fromUser = getLoggedInUser();
        User toUser = validateToUser(dto.getToUserId());
        assertNotSelf(fromUser, toUser, "block");

        List<UserConnection> connections = userConnectionRepository.findAllByFromUserAndToUser(fromUser, toUser);
        assertNoBlockConnection(connections);

        deleteAllConnectionsBetween(fromUser, toUser);
        saveConnection(fromUser, toUser, ConnectionType.BLOCK);
        log.info("User {} blocked user {}", fromUser.getId(), toUser.getId());
        notificationService.sendNotification(fromUser, toUser, NotificationType.BLOCK);
        return ApiResponse.success("Blocked successfully.");
    }


    /**
     * Allows the logged-in user to unblock another user.
     */
    @Override
    public ApiResponse<String> unblockUser(ConnectionRequestDto dto) {
        User fromUser = getLoggedInUser();
        User toUser = validateToUser(dto.getToUserId());
        assertNotSelf(fromUser, toUser, "unblock");

        UserConnection connection = getConnection(fromUser, toUser)
                .filter(c -> c.getConnectionType() == ConnectionType.BLOCK)
                .orElseThrow(() -> new NotBlockedException("You have not blocked this user."));

        userConnectionRepository.delete(connection);
        log.info("User {} unblocked user {}", fromUser.getId(), toUser.getId());
        notificationService.sendNotification(fromUser, toUser, NotificationType.UNBLOCK);
        return ApiResponse.success("Unblocked successfully.");
    }


    /**
     * Get list of users following the current user.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getFollowers() {
        return getConnectionsByType(getLoggedInUser(), false, ConnectionType.FOLLOW, "Followers retrieved.");
    }


    /**
     * Get list of users the current user is following.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getFollowing() {
        return getConnectionsByType(getLoggedInUser(), true, ConnectionType.FOLLOW, "Followings retrieved.");
    }


    /**
     * Get list of users the current user has blocked.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getBlockedUsers() {
        return getConnectionsByType(getLoggedInUser(), true, ConnectionType.BLOCK, "Blocked users retrieved.");
    }


    /**
     * Get followers of a specified user.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getFollowersOfUser(Long userId) {
        return getConnectionsByType(validateToUser(userId), false, ConnectionType.FOLLOW, "Followers retrieved.");
    }


    /**
     * Get followings of a specified user.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getFollowingOfUser(Long userId) {
        return getConnectionsByType(validateToUser(userId), true, ConnectionType.FOLLOW, "Following retrieved.");
    }


    /**
     * Get mutual friends between current user and another user.
     */
    @Override
    public ApiResponse<List<ConnectionResponseDto>> getMutualFriendsBetween(Long otherUserId) {
        User currentUser = getLoggedInUser();
        User otherUser = validateToUser(otherUserId);

        List<Long> mutualIds = userConnectionRepository.findMutualFriendIdsBetweenUsers(currentUser.getId(), otherUser.getId());
        if (mutualIds.isEmpty()) {
            return ApiResponse.success(Collections.emptyList(), "No mutual friends found.");
        }

        List<UserConnection> mutualConnections = userConnectionRepository.findByFromUserAndToUserIdInAndConnectionType(currentUser, mutualIds, ConnectionType.FOLLOW);
        List<ConnectionResponseDto> response = mutualConnections.stream().map(this::toDto).collect(Collectors.toList());
        return ApiResponse.success(response, "Mutual friends retrieved.");
    }


    /**
     * Check if current user is following another user.
     */
    @Override
    public ApiResponse<Boolean> isFollowingUser(Long userId) {
        User currentUser = getLoggedInUser();
        boolean isFollowing = userConnectionRepository.existsByFromUserIdAndToUserIdAndConnectionType(currentUser.getId(), userId, ConnectionType.FOLLOW);
        return ApiResponse.success(isFollowing, "Following status retrieved.");
    }


    // ----------------- PRIVATE UTILITY METHODS -------------------

    private void assertNotSelf(User fromUser, User toUser, String action) {
        if (fromUser.getId().equals(toUser.getId())) {
            throw new InvalidConnectionStateException("You cannot " + action + " yourself.");
        }
    }

    private Optional<UserConnection> getConnection(User fromUser, User toUser) {
        return userConnectionRepository.findByFromUserAndToUser(fromUser, toUser);
    }

    private void deleteAllConnectionsBetween(User fromUser, User toUser) {
        List<UserConnection> connections = userConnectionRepository.findAllByFromUserAndToUser(fromUser, toUser);
        if (!connections.isEmpty()) {
            userConnectionRepository.deleteAll(connections);
            log.info("Deleted all existing connections from user {} to user {}", fromUser.getId(), toUser.getId());
        }
    }

    private void assertNoBlockConnection(List<UserConnection> connections) {
        boolean blocked = connections.stream()
                .anyMatch(conn -> conn.getConnectionType() == ConnectionType.BLOCK);
        if (blocked) {
            throw new AlreadyBlockedException("User already blocked.");
        }
    }

    private void saveConnection(User fromUser, User toUser, ConnectionType type) {
        UserConnection connection = new UserConnection();
        connection.setFromUser(fromUser);
        connection.setToUser(toUser);
        connection.setConnectionType(type);
        userConnectionRepository.save(connection);
    }

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UserNotFoundException("User not found or not enabled with username: " + username));
    }

    private User validateToUser(Long toUserId) {
        return userRepository.findById(toUserId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UserNotFoundException("User not found or not enabled with id: " + toUserId));
    }

    private ConnectionResponseDto toDto(UserConnection connection) {
        return ConnectionResponseDto.builder()
                .fromUserId(connection.getFromUser().getId())
                .toUserId(connection.getToUser().getId())
                .connectionType(connection.getConnectionType().name())
                .createdAt(connection.getCreatedAt())
                .build();
    }

    private ApiResponse<List<ConnectionResponseDto>> getConnectionsByType(User user, boolean fromUser, ConnectionType type, String message) {
        List<UserConnection> connections = fromUser ?
                userConnectionRepository.findByFromUserAndConnectionType(user, type) :
                userConnectionRepository.findByToUserAndConnectionType(user, type);
        List<ConnectionResponseDto> response = connections.stream().map(this::toDto).collect(Collectors.toList());
        return ApiResponse.success(response, message);
    }
}
