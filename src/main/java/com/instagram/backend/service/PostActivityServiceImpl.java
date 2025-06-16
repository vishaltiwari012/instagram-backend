package com.instagram.backend.service;

import com.instagram.backend.dtos.request.CommentRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.CommentResponse;
import com.instagram.backend.entity.*;
import com.instagram.backend.entity.enums.NotificationType;
import com.instagram.backend.exception.*;
import com.instagram.backend.repository.*;
import com.instagram.backend.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostActivityServiceImpl implements PostActivityService{

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final AuthenticationFacade authenticationFacade;
    private final CommentLikeRepository commentLikeRepository;
    private final MentionRepository mentionRepository;

    @Override
    public ApiResponse<String> likePost(Long postId) {
        Post post = getPostOrThrow(postId);
        User currentUser = getLoggedInUser();

        log.info("User [{}] is liking Post [{}]", currentUser.getUsername(), postId);

        if (likeRepository.existsByPostIdAndUserId(postId, currentUser.getId())) {
            throw new AlreadyLikedPostException("Post already liked by you.");
        }

        Like like = new Like();
        like.setPost(post);
        like.setUser(currentUser);
        like.setLikedAt(LocalDateTime.now());
        likeRepository.save(like);

        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        if(!post.getUser().equals(currentUser)) {
            notificationService.sendNotification(currentUser, post.getUser(), NotificationType.LIKE);
        }

        return ApiResponse.success("Post liked successfully.");

    }

    @Override
    @Transactional
    public ApiResponse<String> unlikePost(Long postId) {
        Post post = getPostOrThrow(postId);
        User currentUser = getLoggedInUser();

        log.info("User [{}] is unliking Post [{}]", currentUser.getUsername(), postId);

        if(!likeRepository.existsByPostIdAndUserId(postId, currentUser.getId())) {
            throw new NotLikedPostException("You have not liked this post");
        }

        likeRepository.deleteByPostIdAndUserId(postId, currentUser.getId());
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);

        return ApiResponse.success("Post unliked successfully");
    }

    @Override
    public ApiResponse<CommentResponse> addComment(Long postId, CommentRequest request) {
        Post post = getPostOrThrow(postId);
        User currentUser = getLoggedInUser();

        log.info("User [{}] commented on Post [{}]", currentUser.getUsername(), postId);

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(currentUser);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if(!post.getUser().equals(currentUser)) {
            notificationService.sendNotification(currentUser, post.getUser(), NotificationType.COMMENT);
        }


        // 🔍 Extract @mentions from comment content
        List<String> mentionedUsernames = extractMentions(request.getContent());
        if (!mentionedUsernames.isEmpty()) {
            List<User> mentionedUsers = userRepository.findByUsernameIn(new HashSet<>(mentionedUsernames));
            for (User mentionedUser : mentionedUsers) {
                if (!mentionedUser.equals(currentUser)) {
                    // Save mention
                    Mention mention = new Mention();
                    mention.setMentionedUser(mentionedUser);
                    mention.setPost(post);
                    mention.setComment(savedComment);
                    mention.setCreatedAt(Instant.now());
                    mentionRepository.save(mention);

                    // Send notification
                    notificationService.sendNotification(currentUser, mentionedUser, NotificationType.MENTIONED_IN_COMMENT);
                }
            }
        }

        return ApiResponse.success(mapToDto(savedComment, currentUser),"Comment added successfully.");
    }

    @Override
    public ApiResponse<List<CommentResponse>> getCommentsForPost(Long postId) {
        Post post = getPostOrThrow(postId);
        User currentUser = getLoggedInUser();

        // Fetch only top-level comments
        List<Comment> topLevelComments = commentRepository.findByPostIdAndParentIsNullOrderByCommentedAtAsc(postId);


        List<CommentResponse> response = topLevelComments.stream()
                .map(comment -> mapToDto(comment, currentUser))
                .toList();

        return ApiResponse.success(response, "Comments fetched successfully.");
    }

    @Override
    public ApiResponse<String> likeComment(Long commentId) {
        User currentUser = getLoggedInUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        boolean alreadyLiked = commentLikeRepository.existsByCommentAndUser(comment, currentUser);
        if (alreadyLiked) {
            throw new CommentAlreadyLikedException("You have already liked this comment.");
        }

        CommentLike like = new CommentLike();
        like.setComment(comment);
        like.setUser(currentUser);
        like.setLikedAt(LocalDateTime.now());

        commentLikeRepository.save(like);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);

        return ApiResponse.success("Comment liked successfully.");

    }

    @Override
    public ApiResponse<String> unlikeComment(Long commentId) {
        User currentUser = getLoggedInUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        boolean alreadyLiked = commentLikeRepository.existsByCommentAndUser(comment, currentUser);
        if (!alreadyLiked) {
            throw new CommentNotLikedException("You haven't liked this comment yet.");
        }
        commentLikeRepository.deleteByCommentAndUser(comment, currentUser);
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentRepository.save(comment);

        return ApiResponse.success("Comment unliked successfully.");
    }

    // Utility methods
    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        log.debug("Authenticated username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }

    private CommentResponse mapToDto(Comment comment, User currentUser) {
        List<CommentResponse> replies = comment.getReplies().stream()
                .map(reply -> mapToDto(reply, currentUser))
                .collect(Collectors.toList());

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getUsername(),
                comment.getCommentedAt(),
                replies,
                commentLikeRepository.countByComment(comment),
                commentLikeRepository.existsByCommentAndUser(comment, currentUser)
        );
    }

    private List<String> extractMentions(String text) {
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(text);
        List<String> mentions = new ArrayList<>();
        while (matcher.find()) mentions.add(matcher.group(1));
        return mentions;
    }
}
