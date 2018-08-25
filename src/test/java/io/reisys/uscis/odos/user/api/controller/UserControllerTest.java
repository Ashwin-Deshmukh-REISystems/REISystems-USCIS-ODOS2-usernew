package io.reisys.uscis.odos.user.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.reisys.uscis.odos.user.api.controller.UserController;
import io.reisys.uscis.odos.user.api.model.User;
import io.reisys.uscis.odos.user.api.util.AccessManagementUtil;
import io.reisys.uscis.odos.user.api.util.OktaClientUtil;
import io.reisys.uscis.odos.user.api.utils.UserTestUtility;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserControllerTest {

	@InjectMocks
	private UserController userController;
	
	@Mock
	private OktaClientUtil oktaClientUtil;
	
	@Mock
	private AccessManagementUtil accessManagementUtil;
	
	private MockMvc mockMvc;
	
	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
	}
	
	@Test
	public void testGetUsers() throws Exception {
		//Invalid Access Token
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(get("/api/v1/user").header("X-Auth-Token", "abc"))
			.andExpect(status().isUnauthorized());
		
		//Valid Access Token
		User user1 = UserTestUtility.createUser("1", "FirstName1", "LastName1", "FirstName1.LastName1@test.com");
		User user2 = UserTestUtility.createUser("2", "FirstName2", "LastName", "FirstName2.LastName2@test.com");
		List<User> users = Arrays.asList(user1, user2);
		when(oktaClientUtil.getAllActiveUsers()).thenReturn(users);
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("userId");
		
		mockMvc.perform(get("/api/v1/user").header("X-Auth-Token", "abc"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
			.andExpect(jsonPath("$.content", hasSize(2)));
		
		Mockito.verify(oktaClientUtil, times(1)).getAllActiveUsers();
		
		//When No Active Users exist
		when(oktaClientUtil.getAllActiveUsers()).thenReturn(null);
		mockMvc.perform(get("/api/v1/user").header("X-Auth-Token", "abc"))
			.andExpect(status().isNotFound());

	}
	
	@Test
	public void testGetUserForUserId() throws Exception {
		//Invalid Access Token
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(get("/api/v1/user/1").header("X-Auth-Token", "abc"))
			.andExpect(status().isUnauthorized());
		
		//Valid Token
		User user1 = UserTestUtility.createUser("1", "FirstName1", "LastName1", "FirstName1.LastName1@test.com");
		User user2 = UserTestUtility.createUser("2", "FirstName2", "LastName2", "FirstName2.LastName2@test.com");
		when(oktaClientUtil.getUser("1")).thenReturn(user1);
		when(oktaClientUtil.getUser("2")).thenReturn(user2);
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("userId");
		
		mockMvc.perform(get("/api/v1/user/1").header("X-Auth-Token", "abc"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
			.andExpect(jsonPath("$.userId", is(user1.getUserId())))
			.andExpect(jsonPath("$.firstName", is(user1.getFirstName())))
			.andExpect(jsonPath("$.lastName", is(user1.getLastName())))
			.andExpect(jsonPath("$.email", is(user1.getEmail())))
			.andExpect(jsonPath("$.status", is(user1.getStatus())));
		Mockito.verify(oktaClientUtil, times(1)).getUser("1");
		
		mockMvc.perform(get("/api/v1/user/2").header("X-Auth-Token", "abc"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
			.andExpect(jsonPath("$.userId", is(user2.getUserId())))
			.andExpect(jsonPath("$.firstName", is(user2.getFirstName())))
			.andExpect(jsonPath("$.lastName", is(user2.getLastName())))
			.andExpect(jsonPath("$.email", is(user2.getEmail())))
			.andExpect(jsonPath("$.status", is(user2.getStatus())));
		Mockito.verify(oktaClientUtil, times(1)).getUser("2");
		
		//When User with id does not exist
		when(oktaClientUtil.getUser("3")).thenReturn(null);
		mockMvc.perform(get("/api/v1/user/3").header("X-Auth-Token", "abc"))
			.andExpect(status().isNotFound());
	}
	
	@Test
	public void testCreateUser() throws Exception {
		//Invalid Access Token
		User user1 = UserTestUtility.createUser("1", "FirstName1", "LastName1", "FirstName1.LastName1@test.com");
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(post("/api/v1/user/create").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
			.andExpect(status().isUnauthorized());
		
		//Valid Access Token
		
		when(oktaClientUtil.createUser(user1)).thenReturn(user1);
		when(oktaClientUtil.doesUserWithEmailExist("FirstName1.LastName1@test.com")).thenReturn(Boolean.FALSE);
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("userId");
		
		mockMvc.perform(post("/api/v1/user/create").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(user1.getUserId())))
				.andExpect(jsonPath("$.firstName", is(user1.getFirstName())))
				.andExpect(jsonPath("$.lastName", is(user1.getLastName())))
				.andExpect(jsonPath("$.email", is(user1.getEmail())))
				.andExpect(jsonPath("$.status", is(user1.getStatus())));
		
		Mockito.verify(oktaClientUtil, times(1)).createUser(user1);
		
		when(oktaClientUtil.doesUserWithEmailExist("FirstName1.LastName1@test.com")).thenReturn(Boolean.TRUE);
		mockMvc.perform(post("/api/v1/user/create").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
				.andExpect(status().isNotFound());
		
	}
	
	@Test
	public void testUpdateUser() throws Exception {
		//Invalid Access Token
		User user1 = UserTestUtility.createUser("1", "FirstName1", "LastName1", "FirstName1.LastName1@test.com");
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(patch("/api/v1/user/1/update").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
			.andExpect(status().isUnauthorized());
		
		//Valid Access Token
		when(oktaClientUtil.updateUser(user1)).thenReturn(Boolean.TRUE);
		when(oktaClientUtil.getUser("1")).thenReturn(user1);
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("1");
		
		mockMvc.perform(patch("/api/v1/user/1/update").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
			.andExpect(status().isOk());
		
		Mockito.verify(oktaClientUtil, times(1)).updateUser(user1);
		Mockito.verify(oktaClientUtil, times(1)).getUser("1");
		
		//When update was not successful
		when(oktaClientUtil.updateUser(user1)).thenReturn(Boolean.FALSE);
		mockMvc.perform(patch("/api/v1/user/1/update").header("X-Auth-Token", "abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UserTestUtility.getUserAsJsonString(user1)))
			.andExpect(status().isNotFound());
	}
	
	@Test
	public void testDeleteUser() throws Exception {
		//Invalid Access Token
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(delete("/api/v1/user/1/delete").header("X-Auth-Token", "abc"))
			.andExpect(status().isUnauthorized());
		
		//Valid Access Token
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("userId");
		when(oktaClientUtil.deActivateUser("1")).thenReturn(Boolean.TRUE);
		
		mockMvc.perform(delete("/api/v1/user/1/delete").header("X-Auth-Token", "abc"))
			.andExpect(status().isOk());
		
		Mockito.verify(oktaClientUtil, times(1)).deActivateUser("1");
		
		//When Delete was not successful
		when(oktaClientUtil.deActivateUser("2")).thenReturn(Boolean.FALSE);
		
		mockMvc.perform(delete("/api/v1/user/2/delete").header("X-Auth-Token", "abc"))
			.andExpect(status().isNotFound());
		
		Mockito.verify(oktaClientUtil, times(1)).deActivateUser("2");
	}
	
	@Test
	public void testActivateUser() throws Exception {
		//Invalid Access Token
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn(null);
		mockMvc.perform(delete("/api/v1/user/1/activate").header("X-Auth-Token", "abc"))
			.andExpect(status().isUnauthorized());
		
		//Valid Access Token
		when(oktaClientUtil.activateUser("1")).thenReturn(Boolean.TRUE);
		when(accessManagementUtil.getUserIdFromToken("abc")).thenReturn("userId");
		mockMvc.perform(delete("/api/v1/user/1/activate").header("X-Auth-Token", "abc"))
			.andExpect(status().isOk());
		
		Mockito.verify(oktaClientUtil, times(1)).activateUser("1");
		
		//When Delete was not successful
		when(oktaClientUtil.activateUser("2")).thenReturn(Boolean.FALSE);
		
		mockMvc.perform(delete("/api/v1/user/2/activate").header("X-Auth-Token", "abc"))
			.andExpect(status().isNotFound());
		
		Mockito.verify(oktaClientUtil, times(1)).activateUser("2");
	}
}
