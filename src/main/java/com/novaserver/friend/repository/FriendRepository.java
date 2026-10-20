package com.novaserver.friend.repository;

import com.novaserver.friend.FriendStatus;
import com.novaserver.friend.entity.Friend;
import com.novaserver.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 친구 신청·관계 저장소. */
public interface FriendRepository extends JpaRepository<Friend, Long> {
    /** 상태가 status이고 userId가 requester·receiver 어느 쪽이든 걸리는 관계를 최근 갱신순으로 반환한다. */
    @EntityGraph(attributePaths = {"requester", "receiver"})
    @Query(
            "select f from Friend f where f.status = :status "
                    + "and (f.requester.id = :userId or f.receiver.id = :userId) "
                    + "order by f.updatedAt desc")
    List<Friend> findAllByStatusAndEitherUser(
            @Param("status") FriendStatus status, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"requester"})
    List<Friend> findAllByReceiverIdAndStatusOrderByCreatedAtDesc(
            Long receiverId, FriendStatus status);

    /** 두 유저 사이의 신청·친구 관계를 쓰기 잠금을 걸고 가져온다. 양방향 동시 신청으로 중복 관계가 생기는 것을 막는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select f from Friend f where (f.requester.id = :userId1 and f.receiver.id = :userId2) "
                    + "or (f.requester.id = :userId2 and f.receiver.id = :userId1)")
    Optional<Friend> findBetweenUsersForUpdate(
            @Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /** 수락·거절 처리용으로 신청 행에 쓰기 잠금을 걸고 가져온다. 받는 사람 본인만 처리할 수 있도록 receiverId로 소유를 확인한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"requester"})
    @Query("select f from Friend f where f.id = :id and f.receiver.id = :receiverId")
    Optional<Friend> findByIdAndReceiverIdForUpdate(
            @Param("id") Long id, @Param("receiverId") Long receiverId);

    /** 친구 찾기용으로, 나와 신청·친구 관계가 없는 유저 중 최대 limit명을 무작위로 반환한다. */
    @Query(
            value =
                    "select u.* from users u "
                            + "where u.id <> :userId "
                            + "and u.id not in ("
                            + "select case when f.requester_id = :userId then f.receiver_id "
                            + "else f.requester_id end "
                            + "from friend f "
                            + "where f.requester_id = :userId or f.receiver_id = :userId) "
                            + "order by rand() limit :limit",
            nativeQuery = true)
    List<User> findRandomNonFriends(@Param("userId") Long userId, @Param("limit") int limit);
}
