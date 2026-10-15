package com.novaserver.user.repository;

import com.novaserver.user.entity.UserEnergy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 유저 에너지 저장소. */
public interface UserEnergyRepository extends JpaRepository<UserEnergy, Long> {
    Optional<UserEnergy> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserEnergy e where e.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
