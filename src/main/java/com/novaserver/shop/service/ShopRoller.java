package com.novaserver.shop.service;

import com.novaserver.shop.dto.ShopPolicy;
import com.novaserver.shop.dto.ShopStock;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/** 서버에서 한 번 추첨한 상품·할인을 저장한다. 클라는 로컬 난수를 다시 계산하지 않는다. */
@Component
public class ShopRoller {
    private final SecureRandom random = new SecureRandom();

    /** 후보를 중복 없이 섞고 첫 슬롯은 무료로 확정한다. */
    public List<ShopStock> roll(ShopPolicy policy) {
        var pool = new ArrayList<>(policy.pool());
        Collections.shuffle(pool, random);
        var result = new ArrayList<ShopStock>();
        for (int slot = 0; slot < policy.slots(); slot++) {
            var offer = pool.get(slot);
            int discount =
                    slot == 0
                            ? 100
                            : policy.discounts().get(random.nextInt(policy.discounts().size()));
            int price = (int) ((long) offer.price() * (100 - discount) / 100);
            result.add(new ShopStock(slot, discount, price, offer));
        }
        return List.copyOf(result);
    }
}
