package com.instagram.backend.repository;

import com.instagram.backend.entity.Post;
import com.instagram.backend.entity.SavedPost;
import com.instagram.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {
    boolean existsByUserAndPost(User user, Post post);
    void deleteByUserAndPost(User user, Post post);
    List<SavedPost> findByUserOrderBySavedAtDesc(User user);
}
