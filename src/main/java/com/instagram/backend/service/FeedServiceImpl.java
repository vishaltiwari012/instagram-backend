package com.instagram.backend.service;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import com.instagram.backend.entity.HashTag;
import com.instagram.backend.entity.Post;
import com.instagram.backend.entity.User;
import com.instagram.backend.exception.NoFollowedUsersException;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.PostRepository;
import com.instagram.backend.repository.UserConnectionRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService{

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public ApiResponse<List<PostResponseDto>> getUserFeed(Instant lastFetched, int size) {
        User currentUser = getLoggedInUser();

        List<User> followedUsers = userConnectionRepository.findFollowedUsers(currentUser.getId());
        if (followedUsers.isEmpty()) {
            throw new NoFollowedUsersException("You are not following any users yet.");
        }

        Instant cursor = (lastFetched != null) ? lastFetched : Instant.now();
        Pageable pageable = PageRequest.of(0, size);

        List<Post> posts = postRepository.findFeedPosts(followedUsers, cursor, pageable);
        List<PostResponseDto> response = posts.stream()
                .map(this::mapToDtoWithUsername)
                .toList();
        return ApiResponse.success(response, "Feed fetched successfully");
    }


//    Utility methods

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }

    private PostResponseDto mapToDtoWithUsername(Post post) {
        PostResponseDto dto = new PostResponseDto();

        // Direct fields
        dto.setId(post.getId());
        dto.setCaption(post.getCaption());
        dto.setImageUrl(post.getImageUrl());
        dto.setUsername(post.getUser().getUsername());
        dto.setCreatedAt(post.getCreatedAt());

        // Map hashtags to list of strings
        List<String> hashtags = post.getHashtags()
                .stream()
                .map(HashTag::getName)
                .toList();
        dto.setHashtags(hashtags);

        // Map mentions to list of usernames
        List<String> mentions = post.getMentions()
                .stream()
                .map(m -> m.getMentionedUser().getUsername())
                .toList();
        dto.setMentions(mentions);

        return dto;
    }
}
