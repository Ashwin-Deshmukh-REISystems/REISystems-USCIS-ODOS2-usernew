package io.reisys.uscis.fads.user.api.utils;

import java.util.Arrays;

import com.google.gson.Gson;

import io.reisys.uscis.fads.user.api.model.User;

public class UserTestUtility {

	public static User createUser(String userId, String firstName, String lastName, String email) {
		User user = new User();
		user.setUserId(userId);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setStatus("ACTIVE");
		user.setRoles(Arrays.asList("Requestor"));
		return user;
	}
	
	public static String getUserAsJsonString(User user) {
		Gson gson = new Gson();
		return gson.toJson(user);
	}
	
}
