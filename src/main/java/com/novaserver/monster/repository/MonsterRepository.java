package com.novaserver.monster.repository;

import com.novaserver.monster.entity.Monster;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 몬스터 저장소. */
public interface MonsterRepository extends JpaRepository<Monster, Long> {

    /** 전체 조회 시 몬스터마다 패시브 쿼리가 따로 나가지 않도록 패시브를 함께 가져온다. */
    @EntityGraph(attributePaths = "passives")
    List<Monster> findAllByOrderByIdAsc();
}
