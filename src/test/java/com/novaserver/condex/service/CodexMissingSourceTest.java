package com.novaserver.condex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.novaserver.condex.entity.Codex;
import com.novaserver.condex.entity.MonsterGrade;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class CodexMissingSourceTest {
    @Autowired private ObjectProvider<CodexRewardSource> sources;
    @Autowired private UserCodexService service;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private CodexRepository monsters;

    @Test
    @Transactional
    void actualProductionWiringHasNoUnverifiedRewardFallback() {
        assertThat(sources.getIfAvailable()).isInstanceOf(DatabaseCodexRewardSource.class);
        var account = accounts.saveAndFlush(new User(Provider.GUEST, UUID.randomUUID().toString()));
        users.createInitialState(account);
        var monster = monsters.saveAndFlush(new Codex(59999L, MonsterGrade.NORMAL));
        service.recordVerifiedEncounter(account.getId(), monster.getId());
        assertThatThrownBy(() -> service.claim(account.getId(), monster.getId()))
                .hasMessageContaining("503");
    }
}
