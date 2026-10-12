package com.novaserver.user.repository;

import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 저장소. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndAccountId(Provider provider, String accountId);
}
