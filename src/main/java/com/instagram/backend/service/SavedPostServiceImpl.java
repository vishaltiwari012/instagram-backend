package com.instagram.backend.service;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import com.instagram.backend.entity.Post;
import com.instagram.backend.entity.SavedPost;
import com.instagram.backend.entity.User;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.repository.PostRepository;
import com.instagram.backend.repository.SavedPostRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedPostServiceImpl implements SavedPostService{

    private final UserRepository userRepository;
    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final AuthenticationFacade authenticationFacade;
    private final ModelMapper modelMapper;


    /**
     * Save a post for the currently logged-in user.
     */
    @Override
    public ApiResponse<String> savePost(Long postId) {
        User user = getLoggedInUser();
        Post post = getPostById(postId);
        if (savedPostRepository.existsByUserAndPost(user, post)) {
            throw new IllegalArgumentException("Post already saved");
        }

        SavedPost savedPost = new SavedPost();
        savedPost.setUser(user);
        savedPost.setPost(post);
        savedPostRepository.save(savedPost);
        return ApiResponse.success("Post saved successfully");
    }


    /**
     * Remove a post from saved posts for the currently logged-in user.
     */
    @Override
    public ApiResponse<String> unSavePost(Long postId) {
        User user = getLoggedInUser();
        Post post = getPostById(postId);
        savedPostRepository.deleteByUserAndPost(user, post);
        return ApiResponse.success("Post unsaved successfully");
    }


    /**
     * Get all saved posts for the currently logged-in user.
     */
    @Override
    public ApiResponse<List<PostResponseDto>> getSavedPosts() {
        User user = getLoggedInUser();
        List<PostResponseDto> response = savedPostRepository.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(saved -> mapToDtoWithUsername(saved.getPost()))
                .collect(Collectors.toList());

        return ApiResponse.success(response, "Saved post fetched successfully");
    }


    //===============UTILITY METHODS======================
    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("Post not found with id: {}", postId);
                    return new ResourceNotFoundException("Post not found");
                });
    }

    private PostResponseDto mapToDtoWithUsername(Post post) {
        PostResponseDto dto = modelMapper.map(post, PostResponseDto.class);
        dto.setUsername(post.getUser().getUsername());
        return dto;
    }
}
