package com.novaserver.user.repository;

import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 저장소. */
public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id in :userIds order by u.id")
    List<User> findAllByIdForUpdate(@Param("userIds") Collection<Long> userIds);

    boolean existsByNicknameKeyAndIdNot(String nicknameKey, Long id);

    Optional<User> findByProviderAndAccountId(Provider provider, String accountId);
}
