package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.novaserver.user.Provider;
import com.novaserver.user.dto.UserProfileRequest;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest
@Import(UserProfileService.class)
class UserProfileServiceTest {
    @Autowired private UserRepository users;
    @Autowired private UserProfileService profiles;

    private User user(String account) {
        return users.saveAndFlush(new User(Provider.GUEST, account));
    }

    @Test
    void duplicateNameIsRejectedIgnoringCaseWithoutChangingOtherProfile() {
        User first = user("profile-first");
        User second = user("profile-second");
        profiles.updateProfile(first.getId(), new UserProfileRequest("Alpha", 4));
        String previousName = second.getNickname();
        assertThatThrownBy(
                        () ->
                                profiles.updateProfile(
                                        second.getId(), new UserProfileRequest("alpha", 2)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(second.getNickname()).isEqualTo(previousName);
        assertThat(second.getAvatarId()).isZero();
    }

    @Test
    void uniqueDatabaseConstraintAlsoRejectsConflictsAfterApplicationCheck() {
        User first = user("race-first");
        first.updateProfile("SameName", 0);
        users.saveAndFlush(first);
        User second = new User(Provider.GUEST, "race-second");
        second.updateProfile("samename", 1);
        assertThatThrownBy(() -> users.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatingOwnNameOrAvatarKeepsTheOtherFieldAndReloadsFromStorage() {
        User first = user("own-profile");
        profiles.updateProfile(first.getId(), new UserProfileRequest("Hero", 3));
        var renamed = profiles.updateProfile(first.getId(), new UserProfileRequest("HERO", null));
        assertThat(renamed.nickname()).isEqualTo("HERO");
        assertThat(renamed.avatarId()).isEqualTo(3);
        var changed = profiles.updateProfile(first.getId(), new UserProfileRequest(null, 4));
        assertThat(changed.nickname()).isEqualTo("HERO");
        assertThat(profiles.getProfile(first.getId()).avatarId()).isEqualTo(4);
    }

    @Test
    void invalidPortraitAndNameDoNotChangeStoredIdentity() {
        User first = user("invalid-profile");
        String before = first.getNickname();
        assertThatThrownBy(
                        () ->
                                profiles.updateProfile(
                                        first.getId(), new UserProfileRequest(null, 99)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        error ->
                                assertThat(error.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(
                        () ->
                                profiles.updateProfile(
                                        first.getId(), new UserProfileRequest("@", null)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        error ->
                                assertThat(error.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(first.getNickname()).isEqualTo(before);
        assertThat(first.getAvatarId()).isZero();
    }
}
