package com.comex.usermodule.core.service;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.domain.UserStatus;
import com.comex.usermodule.core.event.UserVerifiedEvent;
import com.comex.usermodule.core.mapper.UserMapper;
import com.comex.usermodule.core.port.EventPublisher;
import com.comex.usermodule.core.port.PasswordEncoder;
import com.comex.usermodule.core.port.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserMapper userMapper;
	private UserVerificationService sut;

	@Captor
	private ArgumentCaptor<User> userCaptor;

	@Captor
	private ArgumentCaptor<UserVerifiedEvent> eventCaptor;

	@BeforeEach
	void setUp() {
		userMapper = new UserMapper(passwordEncoder);
		sut = new UserVerificationService(userRepository, eventPublisher, userMapper);
	}

	@Test
	void testVerify() {
		// GIVEN
		String verificationCode = DEFAULT_VERIFICATION_CODE;
		User pendingUser = pendingUser();
		Long originalId = pendingUser.getId();
		String originalEmail = pendingUser.getEmail();
		String originalUsername = pendingUser.getUsername();
		String originalPassword = pendingUser.getPassword();
		UserStatus originalStatus = pendingUser.getStatus();

		when(userRepository.findByVerificationCode(verificationCode)).thenReturn(pendingUser);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// WHEN
		sut.verify(verificationCode);

		// THEN
		var inOrder = inOrder(userRepository, eventPublisher);
		inOrder.verify(userRepository).findByVerificationCode(verificationCode);
		inOrder.verify(userRepository).save(any(User.class));
		inOrder.verify(eventPublisher).publish(any(UserVerifiedEvent.class));

		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getStatus()).isEqualTo(UserStatus.VERIFIED);
		assertThat(savedUser.getStatus()).isNotEqualTo(originalStatus);
		assertThat(savedUser).isSameAs(pendingUser);
		assertThat(savedUser.getId()).isEqualTo(originalId);
		assertThat(savedUser.getEmail()).isEqualTo(originalEmail);
		assertThat(savedUser.getUsername()).isEqualTo(originalUsername);
		assertThat(savedUser.getPassword()).isEqualTo(originalPassword);

		verify(eventPublisher).publish(eventCaptor.capture());
		UserVerifiedEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent).isNotNull();
		assertThat(publishedEvent.getId()).isEqualTo(pendingUser.getId());
		assertThat(publishedEvent.getVerificationCode()).isEqualTo(pendingUser.getVerificationCode());
	}
}
