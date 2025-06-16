package com.instagram.backend.repository;


import com.instagram.backend.entity.Post;
import com.instagram.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Page<Post> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    int countByUser(User user);

    @Query("SELECT p FROM Post p WHERE p.user IN :followedUsers AND p.createdAt < :lastFetched ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(@Param("followedUsers") List<User> followedUsers,
                             @Param("lastFetched") Instant lastFetched,
                             Pageable pageable);
}

