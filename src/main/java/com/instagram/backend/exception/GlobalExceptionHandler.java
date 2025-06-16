package com.instagram.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponseEntity(new ApiError(
                "Something went wrong!",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                ErrorCode.SOMETHING_WENT_WRONG
        ));
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                ErrorCode.USER_NOT_FOUND
        ));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleEmailUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
        log.warn("Email already exists: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.EMAIL_ALREADY_EXISTS
        ));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.USER_ALREADY_EXISTS
        ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        log.warn("Email not verified: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI(),
                ErrorCode.EMAIL_NOT_VERIFIED
        ));
    }

    @ExceptionHandler(AccountAlreadyVerifiedException.class)
    public ResponseEntity<ApiError> handleAlreadyVerified(AccountAlreadyVerifiedException ex, HttpServletRequest request) {
        log.warn("Account already verified: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.ACCOUNT_ALREADY_VERIFIED
        ));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiError> handleTokenExpired(TokenExpiredException ex, HttpServletRequest request) {
        log.warn("Token expired: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.GONE,
                request.getRequestURI(),
                ErrorCode.TOKEN_EXPIRED
        ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Invalid token: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.INVALID_TOKEN
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                ErrorCode.RESOURCE_NOT_FOUND
        ));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiError> handleRoleNotFoundException(RoleNotFoundException ex, HttpServletRequest request) {
        log.warn("Role not found: {}", ex.getMessage());
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                ErrorCode.ROLE_NOT_FOUND
        );
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI(),
                ErrorCode.BAD_CREDENTIALS
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on URI {}: {}", request.getRequestURI(), ex.getMessage());
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI(),
                ErrorCode.ACCESS_DENIED
        );
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAction(UnauthorizedActionException ex, HttpServletRequest request) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        ApiError apiError = new ApiError(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI(),
                ErrorCode.ACCESS_DENIED
        );
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        return buildErrorResponseEntity(new ApiError(
                "Database constraint violation",
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.DATA_INTEGRITY_VIOLATION
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        log.warn("Validation failed: {}", errors);
        return buildErrorResponseEntity(new ApiError(
                "Validation Failed",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED,
                errors
        ));
    }

    @ExceptionHandler(AlreadyFollowedException.class)
    public ResponseEntity<ApiError> handleAlreadyFollowed(AlreadyFollowedException ex, HttpServletRequest request) {
        log.warn("Already followed: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.ALREADY_FOLLOWING
        ));
    }

    @ExceptionHandler(NotFollowingException.class)
    public ResponseEntity<ApiError> handleNotFollowing(NotFollowingException ex, HttpServletRequest request) {
        log.warn("Not following: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.NOT_FOLLOWING
        ));
    }

    @ExceptionHandler(AlreadyBlockedException.class)
    public ResponseEntity<ApiError> handleAlreadyBlocked(AlreadyBlockedException ex, HttpServletRequest request) {
        log.warn("Already blocked: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                ErrorCode.ALREADY_BLOCKED
        ));
    }

    @ExceptionHandler(NotBlockedException.class)
    public ResponseEntity<ApiError> handleNotBlocked(NotBlockedException ex, HttpServletRequest request) {
        log.warn("Not blocked: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.NOT_BLOCKED
        ));
    }

    @ExceptionHandler(InvalidConnectionStateException.class)
    public ResponseEntity<ApiError> handleInvalidConnection(InvalidConnectionStateException ex, HttpServletRequest request) {
        log.warn("Invalid connection exist: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.INVALID_CONNECTION
        ));
    }

    @ExceptionHandler(NotLikedPostException.class)
    public ResponseEntity<ApiError> handleNotLikePost(NotLikedPostException ex, HttpServletRequest request) {
        log.warn("Post not liked by you: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.POST_NOT_LIKED
        ));
    }

    @ExceptionHandler(AlreadyLikedPostException.class)
    public ResponseEntity<ApiError> handleAlreadyLikedPost(AlreadyLikedPostException ex, HttpServletRequest request) {
        log.warn("Post already liked by you: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.ALREADY_LIKED_POST
        ));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ApiError> handleChatRoomNotFound(ChatRoomNotFoundException ex, HttpServletRequest request) {
        log.warn("Chat room not found: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                ErrorCode.CHAT_ROOM_NOT_FOUND
        ));
    }

    @ExceptionHandler(InvalidSenderException.class)
    public ResponseEntity<ApiError> handleInvalidSender(InvalidSenderException ex, HttpServletRequest request) {
        log.warn("Invalid sender: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.INVALID_SENDER
        ));
    }

    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ApiError> handleInvalidMessage(InvalidMessageException ex, HttpServletRequest request) {
        log.warn("Invalid message: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.INVALID_MESSAGE
        ));
    }

    @ExceptionHandler(CommentAlreadyLikedException.class)
    public ResponseEntity<ApiError> handleCommentAlreadyLiked(CommentAlreadyLikedException ex, HttpServletRequest request) {
        log.warn("Comment like error: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.COMMENT_ALREADY_LIKED
        ));
    }

    @ExceptionHandler(CommentNotLikedException.class)
    public ResponseEntity<ApiError> handleCommentNotLiked(CommentNotLikedException ex, HttpServletRequest request) {
        log.warn("Comment unlike error: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.COMMENT_NOT_LIKED
        ));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS,
                request.getRequestURI(),
                ErrorCode.TOO_MANY_REQUESTS
        ));
    }

    @ExceptionHandler(NoFollowedUsersException.class)
    public ResponseEntity<ApiError> handleNoFollowedUsers(NoFollowedUsersException ex, HttpServletRequest request) {
        log.warn("No followed users found: {}", ex.getMessage());
        return buildErrorResponseEntity(new ApiError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                ErrorCode.NO_FOLLOWED_USERS
        ));
    }



    private ResponseEntity<ApiError> buildErrorResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
