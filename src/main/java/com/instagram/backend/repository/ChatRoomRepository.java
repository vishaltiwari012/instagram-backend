package com.instagram.backend.repository;

import com.instagram.backend.entity.ChatRoom;
import com.instagram.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT c FROM ChatRoom c WHERE (c.userOne = :userA AND c.userTwo = :userB) OR (c.userOne = :userB AND c.userTwo = :userA)")
    Optional<ChatRoom> findByUsers(@Param("userA") User userA, @Param("userB") User userB);

}