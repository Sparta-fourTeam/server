package com.novaserver.user.repository;

import com.novaserver.user.entity.UserEnergy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 유저 에너지 저장소. */
public interface UserEnergyRepository extends JpaRepository<UserEnergy, Long> {
    Optional<UserEnergy> findByUser_Id(Long userId);
}
