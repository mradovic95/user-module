package com.comex.usermodule.endpoint.controller;

import com.comex.usermodule.core.domain.User;
import com.comex.usermodule.core.service.UserAuthenticationService;
import com.comex.usermodule.core.service.UserService;
import com.comex.usermodule.core.service.UserVerificationService;
import com.comex.usermodule.endpoint.mapper.UserWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.stream.Stream;

import static com.comex.usermodule.core.helper.UserTestInventory.*;
import static com.comex.usermodule.endpoint.helper.EndpointTestInventory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
	controllers = UserController.class,
	excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({UserController.class, UserWebMapper.class})
class UserControllerTest {

	@Autowired
	private MockMvc sut;

	@MockBean
	private UserService userService;

	@MockBean
	private UserAuthenticationService userAuthenticationService;

	@MockBean
	private UserVerificationService userVerificationService;

	// ==================== CREATE USER TESTS ====================

	@Test
	void testCreateUser() throws Exception {
		// GIVEN
		String requestBody = """
			{
				"username": "john_doe",
				"password": "password123",
				"email": "john@example.com"
			}
			""";

		// WHEN & THEN
		sut.perform(post("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated());

		verify(userService).createUser(any());
	}

	@ParameterizedTest
	@MethodSource("variousCreateUserRequests")
	void testCreateUserWithVariousInputs(String requestBody) throws Exception {
		// GIVEN - various request bodies provided by method source

		// WHEN & THEN
		sut.perform(post("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated());

		verify(userService, atLeastOnce()).createUser(any());
	}

	static Stream<String> variousCreateUserRequests() {
		return Stream.of(
			"{}",  // Empty request
			"{\"username\": null, \"password\": null, \"email\": null}",  // Null values
			"{\"username\": \"\", \"password\": \"\", \"email\": \"\"}"  // Empty strings
		);
	}

	// ==================== LOGIN TESTS ====================

	@Test
	void testLogin() throws Exception {
		// GIVEN
		when(userAuthenticationService.login(any()))
			.thenReturn(VALID_JWT_TOKEN);

		// WHEN & THEN
		sut.perform(post("/user/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "john@example.com",
						"password": "password123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").value(VALID_JWT_TOKEN));

		verify(userAuthenticationService).login(any());
	}

	// ==================== VERIFY TESTS ====================

	@Test
	void testVerify() throws Exception {
		// GIVEN
		String verificationCode = "ABC123";
		doNothing().when(userVerificationService).verify(verificationCode);

		// WHEN & THEN
		sut.perform(get("/user/verify")
				.param("code", verificationCode))
			.andExpect(status().isOk());

		verify(userVerificationService).verify(verificationCode);
	}

	// ==================== FIND BY EMAIL TESTS ====================

	@Test
	void testFindByEmail() throws Exception {
		// GIVEN
		User user = verifiedUser();
		when(userService.findByEmail(DEFAULT_EMAIL)).thenReturn(user);

		// WHEN & THEN
		sut.perform(get("/user")
				.param("email", DEFAULT_EMAIL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(DEFAULT_USER_ID))
			.andExpect(jsonPath("$.username").value(DEFAULT_USERNAME))
			.andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
			.andExpect(jsonPath("$.status").value("VERIFIED"))
			.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

		verify(userService).findByEmail(DEFAULT_EMAIL);
	}

	@Test
	void testFindByEmailWithMultipleRoles() throws Exception {
		// GIVEN
		User user = userBuilder()
			.roles(Set.of(userRole(), adminRole()))
			.build();
		when(userService.findByEmail(DEFAULT_EMAIL)).thenReturn(user);

		// WHEN & THEN
		sut.perform(get("/user")
				.param("email", DEFAULT_EMAIL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles").isArray())
			.andExpect(jsonPath("$.roles.length()").value(2));

		verify(userService).findByEmail(DEFAULT_EMAIL);
	}
}
