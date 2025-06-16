package com.instagram.backend.repository;

import com.instagram.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
//    List<Comment> findByPostId(Long postId);
      List<Comment> findByPostIdAndParentIsNullOrderByCommentedAtAsc(Long postId);
}
