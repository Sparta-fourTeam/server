package com.novaserver.stage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스테이지. 몬스터·웨이브 구성은 별도 작업에서 채운다. */
@Entity
@Table(name = "stage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stage {
    @Id private Integer stageId;

    /** 주어진 stageId로 스테이지를 만든다. */
    public static Stage create(Integer stageId) {
        Stage stage = new Stage();
        stage.stageId = stageId;
        return stage;
    }
}
