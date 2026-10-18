-- MySQL 8 전용 수동 전환. 애플리케이션/배포에서 자동 실행하지 않는다.
-- 운영 적용은 별도 승인 후 애플리케이션을 중지한 상태에서 수행한다.
-- 이전 숫자 튜토리얼 정의와 완료 기록만 초기화한다. 문자열 ID로 기존 행을 추측 이전하지 않는다.
-- 계정·경험치·재화·아이템·별등급 등 다른 테이블과 데이터는 변경하지 않는다.
-- 선행 조건: #77의 reward_definition 및 users/stage/upgrade 스키마 적용 완료.
-- 선택한 DB에서 실행하며, 이미 문자열로 전환된 DB의 재실행은 데이터 삭제 전에 거절한다.
-- 적용 후 Hibernate validate로 확인하고 출시 튜토리얼/고정 보상 정의를 준비해야 한다.

DROP PROCEDURE IF EXISTS rebuild_legacy_tutorial_state;
DELIMITER //
CREATE PROCEDURE rebuild_legacy_tutorial_state()
BEGIN
    IF COALESCE((SELECT data_type FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'tutorials'
                AND column_name = 'id'), '') <> 'bigint'
        OR COALESCE((SELECT data_type FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'user_tutorial'
                AND column_name = 'tutorial_id'), '') <> 'bigint' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected legacy BIGINT tutorial IDs; no data changed';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
            AND table_name IN ('users', 'stage', 'upgrade', 'reward_definition')) <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Apply prerequisite server schemas before rebuilding tutorials';
    END IF;

    DROP TABLE user_tutorial;
    DROP TABLE tutorials;

    CREATE TABLE `tutorials` (
      `clear_stage_stage_id` int DEFAULT NULL,
      `min_level` int NOT NULL,
      `player_level` int NOT NULL,
      `set_level` int NOT NULL,
      `start_stage_stage_id` int DEFAULT NULL,
      `upgrade_level` int NOT NULL,
      `reward_definition_id` bigint NOT NULL,
      `id` varchar(64) NOT NULL,
      `prerequisite_id` varchar(64) DEFAULT NULL,
      `required_upgrade_upgrade_id` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`id`),
      KEY `FKs9ubo9ldtd3slk9tr2j92caqd` (`clear_stage_stage_id`),
      KEY `FK83lay6dc0s485m0i2g9jco43u` (`prerequisite_id`),
      KEY `FK22pbaq4hc8a7qkyfcia96dlxm` (`required_upgrade_upgrade_id`),
      KEY `FKggxeo76h2lknlj0uqjx5p0pwa` (`reward_definition_id`),
      KEY `FK3i3p70irety8hy1oni11mvusj` (`start_stage_stage_id`),
      CONSTRAINT `FK22pbaq4hc8a7qkyfcia96dlxm` FOREIGN KEY (`required_upgrade_upgrade_id`) REFERENCES `upgrade` (`upgrade_id`),
      CONSTRAINT `FK3i3p70irety8hy1oni11mvusj` FOREIGN KEY (`start_stage_stage_id`) REFERENCES `stage` (`stage_id`),
      CONSTRAINT `FK83lay6dc0s485m0i2g9jco43u` FOREIGN KEY (`prerequisite_id`) REFERENCES `tutorials` (`id`),
      CONSTRAINT `FKggxeo76h2lknlj0uqjx5p0pwa` FOREIGN KEY (`reward_definition_id`) REFERENCES `reward_definition` (`id`),
      CONSTRAINT `FKs9ubo9ldtd3slk9tr2j92caqd` FOREIGN KEY (`clear_stage_stage_id`) REFERENCES `stage` (`stage_id`)
    ) ENGINE=InnoDB;

    CREATE TABLE `user_tutorial` (
      `exp_after` int NOT NULL,
      `exp_before` int NOT NULL,
      `gold` int NOT NULL,
      `completed_at` datetime(6) DEFAULT NULL,
      `dialogue_seen_at` datetime(6) DEFAULT NULL,
      `id` bigint NOT NULL AUTO_INCREMENT,
      `user_id` bigint NOT NULL,
      `tutorial_id` varchar(64) NOT NULL,
      `items` json NOT NULL,
      PRIMARY KEY (`id`),
      UNIQUE KEY `UKdyvxk9s7sfgf59dt4p11e6cty` (`user_id`,`tutorial_id`),
      KEY `FK41h6rbp53cam88lk04ke86y3c` (`tutorial_id`),
      CONSTRAINT `FK41h6rbp53cam88lk04ke86y3c` FOREIGN KEY (`tutorial_id`) REFERENCES `tutorials` (`id`),
      CONSTRAINT `FK5mq9g7lt0b3sd0cqevt64pomi` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ) ENGINE=InnoDB;
END//
CALL rebuild_legacy_tutorial_state()//
DROP PROCEDURE rebuild_legacy_tutorial_state//
DELIMITER ;
