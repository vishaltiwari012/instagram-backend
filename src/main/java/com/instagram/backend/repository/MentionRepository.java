package com.instagram.backend.repository;

import com.instagram.backend.entity.Mention;
import com.instagram.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentionRepository extends JpaRepository<Mention, Long> {
    List<Mention> findByMentionedUser(User user);
}
