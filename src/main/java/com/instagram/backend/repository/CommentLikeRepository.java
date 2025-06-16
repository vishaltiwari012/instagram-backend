package com.instagram.backend.repository;

import com.instagram.backend.entity.Comment;
import com.instagram.backend.entity.CommentLike;
import com.instagram.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentAndUser(Comment comment, User user);
    int countByComment(Comment comment);
    void deleteByCommentAndUser(Comment comment, User user);
}
