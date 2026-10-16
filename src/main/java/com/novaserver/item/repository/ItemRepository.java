package com.novaserver.item.repository;

import com.novaserver.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

/** 아이템 저장소. */
public interface ItemRepository extends JpaRepository<Item, String> {}
