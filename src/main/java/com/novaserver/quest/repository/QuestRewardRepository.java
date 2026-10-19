package com.novaserver.quest.repository;

import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.QuestReward;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 퀘스트 보상 정의 저장소. */
public interface QuestRewardRepository extends JpaRepository<QuestReward, String> {
    List<QuestReward> findByPeriodOrderByPoints(QuestPeriod period);

    List<QuestReward> findByPeriodAndPointsBetween(
            QuestPeriod period, int fromPoints, int toPoints);

    boolean existsByPeriodAndPoints(QuestPeriod period, int points);
}
