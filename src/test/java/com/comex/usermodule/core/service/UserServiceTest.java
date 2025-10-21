package com.comex.usermodule.core.service;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.dto.CreateUserDto;
import com.comex.usermodule.core.event.UserCreatedEvent;
import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.stream.Stream;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    private UserMapper userMapper;
    private UserService sut;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<UserCreatedEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        userMapper = new UserMapper(passwordEncoder);
        sut = new UserService(true, userRepository, eventPublisher, userMapper);
    }

    @ParameterizedTest
    @MethodSource("provideVerificationScenarios")
    void testCreateUser(boolean verificationRequired, UserStatus expectedStatus) {
        // GIVEN
        CreateUserDto createUserDto = createUserDto("testuser", "password123", "test@example.com");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        sut = new UserService(verificationRequired, userRepository, eventPublisher, userMapper);
        // WHEN
        User result = sut.createUser(createUserDto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().iterator().next().getName()).isEqualTo("ROLE_USER");

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getStatus()).isEqualTo(expectedStatus);

        verify(eventPublisher).publish(eventCaptor.capture());
        UserCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getUsername()).isEqualTo("testuser");
        assertThat(publishedEvent.getEmail()).isEqualTo("testuser");
    }

    private static Stream<Object[]> provideVerificationScenarios() {
        return Stream.of(
                new Object[]{false, UserStatus.VERIFIED},
                new Object[]{true, UserStatus.CREATED}
        );
    }

    @Test
    void testFindByEmail() {
        // GIVEN
        String email = DEFAULT_EMAIL;
        User expectedUser = userWithEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(expectedUser);

        // WHEN
        User result = sut.findByEmail(email);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedUser);
        assertThat(result.getEmail()).isEqualTo(email);

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @MethodSource("provideFindByEmailOptionalScenarios")
    void testFindByEmailOptional(String email, boolean shouldExist) {
        // GIVEN
        User expectedUser = shouldExist ? userWithEmail(email) : null;
        Optional<User> optionalUser = Optional.ofNullable(expectedUser);

        when(userRepository.findByEmailOptional(email)).thenReturn(optionalUser);

        // WHEN
        Optional<User> result = sut.findByEmailOptional(email);

        // THEN
        if (shouldExist) {
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUser);
            assertThat(result.get().getEmail()).isEqualTo(email);
        } else {
            assertThat(result).isEmpty();
        }

        verify(userRepository, times(1)).findByEmailOptional(email);
        verifyNoInteractions(eventPublisher);
    }

    private static Stream<Object[]> provideFindByEmailOptionalScenarios() {
        return Stream.of(
                new Object[]{DEFAULT_EMAIL, true},
                new Object[]{"nonexistent@test.com", false}
        );
    }
}
