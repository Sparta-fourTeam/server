package com.novaserver.quest.repository;

import com.novaserver.quest.entity.QuestDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 서버 행동 퀘스트 정의 저장소. */
public interface QuestDefinitionRepository extends JpaRepository<QuestDefinition, String> {
    @Query("select q.id from QuestDefinition q where q.id = :id")
    Optional<String> findCanonicalId(@Param("id") String id);
}
