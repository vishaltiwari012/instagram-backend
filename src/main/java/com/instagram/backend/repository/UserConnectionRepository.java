package com.instagram.backend.repository;

import com.instagram.backend.entity.User;
import com.instagram.backend.entity.UserConnection;
import com.instagram.backend.entity.enums.ConnectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {
    Optional<UserConnection> findByFromUserAndToUser(User fromUser, User toUser);
    List<UserConnection> findByToUserAndConnectionType(User toUser, ConnectionType connectionType);
    List<UserConnection> findByFromUserAndConnectionType(User fromUser, ConnectionType connectionType);
    List<UserConnection> findAllByFromUserAndToUser(User fromUser, User toUser);
    int countByToUserAndConnectionType(User toUser, ConnectionType connectionType);
    int countByFromUserAndConnectionType(User fromUser, ConnectionType connectionType);
    @Query("""
    SELECT u.id FROM User u
    WHERE u.id IN (
        SELECT uc1.toUser.id FROM UserConnection uc1
        WHERE uc1.fromUser.id = :userAId AND uc1.connectionType = 'FOLLOW'
        AND uc1.toUser.id IN (
            SELECT uc2.fromUser.id FROM UserConnection uc2
            WHERE uc2.toUser.id = :userAId AND uc2.connectionType = 'FOLLOW'
        )
    )
    AND u.id IN (
        SELECT uc3.toUser.id FROM UserConnection uc3
        WHERE uc3.fromUser.id = :userBId AND uc3.connectionType = 'FOLLOW'
        AND uc3.toUser.id IN (
            SELECT uc4.fromUser.id FROM UserConnection uc4
            WHERE uc4.toUser.id = :userBId AND uc4.connectionType = 'FOLLOW'
        )
    )
""")
    List<Long> findMutualFriendIdsBetweenUsers(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
    List<UserConnection> findByFromUserAndToUserIdInAndConnectionType(User fromUser, List<Long> toUserIds, ConnectionType type);

    @Query("SELECT uc.toUser FROM UserConnection uc WHERE uc.fromUser.id = :userId AND uc.connectionType = 'FOLLOW'")
    List<User> findFollowedUsers(@Param("userId") Long userId);

    boolean existsByFromUserIdAndToUserIdAndConnectionType(Long fromUserId, Long toUserId, ConnectionType connectionType);

}

