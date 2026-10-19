package com.novaserver.quest.repository;

import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 퀘스트 정의 저장소. */
public interface QuestRepository extends JpaRepository<Quest, String> {
    List<Quest> findByPeriodOrderByQuestId(QuestPeriod period);

    List<Quest> findByKind(QuestKind kind);
}
