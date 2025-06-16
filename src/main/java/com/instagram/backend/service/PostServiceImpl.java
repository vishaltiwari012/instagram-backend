package com.instagram.backend.service;

import com.instagram.backend.dtos.request.PostRequestDto;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import com.instagram.backend.entity.HashTag;
import com.instagram.backend.entity.Mention;
import com.instagram.backend.entity.Post;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.enums.NotificationType;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.exception.UnauthorizedActionException;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.HashTagRepository;
import com.instagram.backend.repository.MentionRepository;
import com.instagram.backend.repository.PostRepository;
import com.instagram.backend.repository.UserRepository;
import com.instagram.backend.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AuthenticationFacade authenticationFacade;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;
    private final HashTagRepository hashTagRepository;
    private final MentionRepository mentionRepository;
    private final NotificationService notificationService;


    /**
     * Creates a new post with optional image, hashtags, and mentions.
     */
    @Override
    @Transactional
    public ApiResponse<PostResponseDto> createPost(PostRequestDto postRequest) {
        log.info("Post creation starts...");
        User loggedInUser = getLoggedInUser();

        Post post = new Post();
        post.setUser(loggedInUser);
        post.setCaption(postRequest.getCaption());
        post.setCreatedAt(Instant.now());

        if (postRequest.getImage() != null && !postRequest.getImage().isEmpty()) {
            uploadImageToCloudinary(post, postRequest.getImage());
        }

        Post savedPost = postRepository.save(post);

        // Handle hashtags and mentions
        handleHashtags(postRequest.getCaption(), savedPost);
        handleMentions(postRequest.getCaption(), savedPost, loggedInUser);

        log.info("Post created with ID: {}", savedPost.getId());
        return ApiResponse.success(mapToDtoWithUsername(savedPost), "Post created successfully");
    }


    /**
     * Updates a post's caption and/or image.
     */
    @Override
    @Transactional
    public ApiResponse<PostResponseDto> updatePost(Long postId, PostRequestDto postRequest) {
        User loggedInUser = getLoggedInUser();
        Post post = findPostById(postId);
        validateOwnership(post, loggedInUser, "update");

        post.setCaption(postRequest.getCaption());
        if (postRequest.getImage() != null && !postRequest.getImage().isEmpty()) {
            if (post.getImagePublicId() != null) {
                deleteCloudinaryImage(post.getImagePublicId());
            }
            uploadImageToCloudinary(post, postRequest.getImage());
        }

        Post updatedPost = postRepository.save(post);
        log.info("Post updated: {}", postId);
        return ApiResponse.success(mapToDtoWithUsername(updatedPost), "Post updated successfully");
    }


    /**
     * Deletes a post.
     */
    @Override
    @Transactional
    public ApiResponse<String> deletePost(Long postId) {
        User loggedInUser = getLoggedInUser();
        Post post = findPostById(postId);
        validateOwnership(post, loggedInUser, "delete");

        if (post.getImagePublicId() != null) {
            deleteCloudinaryImage(post.getImagePublicId());
        }

        postRepository.delete(post);
        log.info("Post deleted: {}", postId);
        return ApiResponse.success("Post deleted successfully");
    }


    /**
     * Fetches a post by its ID.
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PostResponseDto> getPostById(Long postId) {
        return ApiResponse.success(mapToDtoWithUsername(findPostById(postId)), "Post fetched successfully");
    }


    /**
     * Fetches all posts.
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PostResponseDto>> getAllPosts() {
        List<PostResponseDto> posts = postRepository.findAll().stream()
                .map(this::mapToDtoWithUsername)
                .toList();
        return ApiResponse.success(posts, "All posts fetched successfully");
    }


    /**
     * Fetches posts of the logged-in user.
     */
    @Override
    public ApiResponse<List<PostResponseDto>> getPostsOfLoggedInUser() {
        User loggedInUser = getLoggedInUser();
        List<PostResponseDto> posts = postRepository.findByUser_IdOrderByCreatedAtDesc(loggedInUser.getId()).stream()
                .map(this::mapToDtoWithUsername)
                .toList();
        return ApiResponse.success(posts, "User's posts fetched successfully");
    }


    /**
     * Fetches posts by a specific user ID.
     */
    @Override
    public ApiResponse<List<PostResponseDto>> getPostsByUser(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        validateProfileVisibility(targetUser, getLoggedInUser());

        List<PostResponseDto> posts = postRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDtoWithUsername)
                .toList();
        return ApiResponse.success(posts, "User's posts fetched successfully");
    }


    /**
     * Fetches paginated posts of a user.
     */
    @Override
    public ApiResponse<Page<PostResponseDto>> getPostsByUser(Long userId, Pageable pageable) {
        return ApiResponse.success(postRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDtoWithUsername), "Paginated posts fetched");
    }


    @Override
    public ApiResponse<List<PostResponseDto>> getMentionedPosts(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        validateProfileVisibility(targetUser, getLoggedInUser());

        List<PostResponseDto> response = mentionRepository.findByMentionedUser(targetUser).stream()
                .map(Mention::getPost)
                .distinct()
                .map(this::mapToDtoWithUsername)
                .toList();

        return ApiResponse.success(response, "Mentioned posts fetched");
    }

    @Override
    public ApiResponse<List<PostResponseDto>> getPostsByHashtag(String tag) {
        HashTag hashtag = hashTagRepository.findByName(tag)
                .orElseThrow(() -> new ResourceNotFoundException("Hashtag not found: " + tag));
        List<PostResponseDto> response = hashtag.getPosts().stream()
                .map(this::mapToDtoWithUsername)
                .toList();
        return ApiResponse.success(response, "Posts with hashtag fetched");
    }

    // ----------------- PRIVATE UTILITY METHODS -------------------

    private void uploadImageToCloudinary(Post post, MultipartFile imageFile) {
        try {
            Map<String, Object> result = cloudinaryService.uploadFile(imageFile);
            post.setImagePublicId((String) result.get("public_id"));
            post.setImageUrl(cloudinaryService.getOptimizedImageUrl(post.getImagePublicId()));
            log.info("Image uploaded to Cloudinary: {}", post.getImagePublicId());
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    private void deleteCloudinaryImage(String publicId) {
        try {
            cloudinaryService.deleteFile(publicId);
            log.info("Deleted image from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.warn("Could not delete image from Cloudinary", e);
        }
    }

    private void handleHashtags(String caption, Post savedPost) {
        List<String> hashtags = extractHashtags(caption);
        List<HashTag> hashtagEntities = hashtags.stream()
                .map(tag -> hashTagRepository.findByName(tag).orElseGet(() -> {
                    HashTag newTag = new HashTag();
                    newTag.setName(tag);
                    return hashTagRepository.save(newTag);
                }))
                .peek(tag -> tag.getPosts().add(savedPost))
                .toList();
        savedPost.setHashtags(hashtagEntities);
    }

    private void handleMentions(String caption, Post post, User author) {
        List<Mention> mentionEntities = new ArrayList<>();
        extractMentions(caption).forEach(username -> {
            userRepository.findByUsername(username).ifPresent(mentionedUser -> {
                Mention mention = new Mention();
                mention.setMentionedUser(mentionedUser);
                mention.setPost(post);
                mention.setCreatedAt(Instant.now());

                Mention savedMention = mentionRepository.save(mention);
                mentionEntities.add(savedMention);
                notificationService.sendNotification(author, mentionedUser, NotificationType.MENTIONED_IN_POST);
            });
        });
        post.setMentions(mentionEntities);
    }

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post not found with ID: {}", postId);
                    return new ResourceNotFoundException("Post not found with id : " + postId);
                });
    }

    private void validateOwnership(Post post, User user, String action) {
        if (!post.getUser().getUsername().equals(user.getUsername())) {
            log.warn("User {} tried to {} post {} owned by {}", user.getUsername(), action, post.getId(), post.getUser().getUsername());
            throw new UnauthorizedActionException("You are not authorized to " + action + " this post");
        }
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

    private List<String> extractHashtags(String text) {
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(text);
        List<String> hashtags = new ArrayList<>();
        while (matcher.find()) hashtags.add(matcher.group(1).toLowerCase());
        return hashtags;
    }

    private List<String> extractMentions(String text) {
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(text);
        List<String> mentions = new ArrayList<>();
        while (matcher.find()) mentions.add(matcher.group(1));
        return mentions;
    }

    public void validateProfileVisibility(User targetUser, User currentUser) {
        if (targetUser.isPrivateProfile() && !targetUser.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("This profile is private.");
        }
    }

}
