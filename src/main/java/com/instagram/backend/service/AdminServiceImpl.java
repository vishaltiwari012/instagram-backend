package com.instagram.backend.service;

import com.instagram.backend.dtos.response.AdminUserListDto;
import com.instagram.backend.dtos.response.AdminUserListResponse;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.ConnectionResponseDto;
import com.instagram.backend.entity.Role;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.UserConnection;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.RoleRepository;
import com.instagram.backend.repository.UserConnectionRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final ModelMapper mapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserConnectionRepository userConnectionRepository;

    @Override
    public ApiResponse<AdminUserListResponse> getAllRegisteredUsers() {
        log.info("Fetching all registered users");
        List<AdminUserListDto> userDtoList = userRepository.findAll()
                .stream()
                .map(user -> mapper.map(user, AdminUserListDto.class))
                .collect(Collectors.toList());

        AdminUserListResponse response = new AdminUserListResponse();
        response.setUsers(userDtoList);

        log.info("Total users fetched: {}", userDtoList.size());
        return ApiResponse.success(response, "Users fetched successfully");
    }

    @Override
    public ApiResponse<AdminUserListDto> getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = getUserOrThrow(id);
        AdminUserListDto userDto = mapper.map(user, AdminUserListDto.class);
        return ApiResponse.success(userDto, "User fetched successfully");
    }

    @Override
    public ApiResponse<String> enableUser(Long id) {
        log.info("Enabling user with id: {}", id);
        User user = getUserOrThrow(id);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", id);
        return ApiResponse.success("User enabled successfully");
    }

    @Override
    public ApiResponse<String> disableUser(Long id) {
        log.info("Disabling user with id: {}", id);
        User user = getUserOrThrow(id);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", id);
        return ApiResponse.success("User disabled successfully");
    }

    @Override
    public ApiResponse<String> deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            log.error("User not exists with id: {}", id);
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
        return ApiResponse.success("User deleted successfully");
    }

    @Override
    public ApiResponse<String> changeUserRole(Long id, String roleName) {
        log.info("Changing role for user id: {} to role: {}", id, roleName);
        User user = getUserOrThrow(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    log.error("Role not found: {}", roleName);
                    return new ResourceNotFoundException("Role not found: " + roleName);
                });
        user.setRoles(Set.of(role));
        userRepository.save(user);
        log.info("Role changed successfully for user id: {}", id);
        return ApiResponse.success("User role updated successfully");
    }

    @Override
    public ApiResponse<AdminUserListResponse> searchUsersByUsername(String username) {
        log.info("Searching users by username containing: {}", username);
        List<AdminUserListDto> users = userRepository.findByUsernameContainingIgnoreCase(username)
                .stream()
                .map(user -> mapper.map(user, AdminUserListDto.class))
                .collect(Collectors.toList());

        AdminUserListResponse response = new AdminUserListResponse();
        response.setUsers(users);
        log.info("Search returned {} users", users.size());
        return ApiResponse.success(response, "Search results");
    }

    @Override
    public ApiResponse<List<ConnectionResponseDto>> getAllConnectionsForAdmin() {
        List<UserConnection> connections = userConnectionRepository.findAll();

        List<ConnectionResponseDto> response = connections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.info("Admin fetched all user connections. Total: {}", response.size());
        return ApiResponse.success(response, "All connections retrieved successfully.");
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
    }

    private ConnectionResponseDto toDto(UserConnection connection) {
        return ConnectionResponseDto.builder()
                .fromUserId(connection.getFromUser().getId())
                .toUserId(connection.getToUser().getId())
                .connectionType(connection.getConnectionType().name())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}
