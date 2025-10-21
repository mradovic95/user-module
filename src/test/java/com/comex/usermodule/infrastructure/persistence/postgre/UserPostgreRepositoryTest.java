package com.comex.usermodule.infrastructure.persistence.postgre;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.exception.UserException;
import com.comex.usermodule.core.port.UserRepository;
import com.comex.usermodule.infrastructure.persistence.postgre.jpa.UserPostgreJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPostgreRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository sut;

    @Autowired
    private UserPostgreJpaRepository userJpaRepository;

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteAll();
    }

    @Test
    void testSave() {
        // GIVEN
        User user = verifiedUser();

        // WHEN
        User savedUser = sut.save(user);

        // THEN
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(savedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.VERIFIED);
        assertThat(savedUser.getRoles()).hasSize(1);
    }

    @Test
    void testFindByEmail() {
        // GIVEN
        User user = verifiedUser();
        sut.save(user);

        // WHEN
        User foundUser = sut.findByEmail(user.getEmail());

        // THEN
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void testFindByEmailOptionalWhenExists() {
        // GIVEN
        User user = verifiedUser();
        sut.save(user);

        // WHEN
        Optional<User> found = sut.findByEmailOptional(user.getEmail());

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void testFindByEmailOptionalWhenEmpty() {
        // GIVEN
        User pendingUser = pendingUser();
        sut.save(pendingUser);

        // WHEN
        Optional<User> foundPending = sut.findByEmailOptional(pendingUser.getEmail());
        Optional<User> notFound = sut.findByEmailOptional("nonexistent@test.com");

        // THEN
        assertThat(foundPending).isEmpty();
        assertThat(notFound).isEmpty();
    }

    @Test
    void testFindByEmailThrowsExceptionWhenNotFound() {
        // GIVEN
        String nonExistentEmail = "nonexistent@test.com";

        // WHEN / THEN
        assertThatThrownBy(() -> sut.findByEmail(nonExistentEmail))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testFindByVerificationCode() {
        // GIVEN
        User user = pendingUser();
        sut.save(user);

        // WHEN
        User foundUser = sut.findByVerificationCode(user.getVerificationCode());

        // THEN
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getVerificationCode()).isEqualTo(user.getVerificationCode());
    }

    @Test
    void testFindByVerificationCodeThrowsExceptionWhenNotFound() {
        // GIVEN
        String nonExistentCode = "NONEXISTENT";

        // WHEN / THEN
        assertThatThrownBy(() -> sut.findByVerificationCode(nonExistentCode))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not found");
    }
}
