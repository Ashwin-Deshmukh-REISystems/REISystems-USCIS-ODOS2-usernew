package io.reisys.uscis.fads.user.api.model;

import org.springframework.hateoas.ResourceSupport;


public class User  extends ResourceSupport {

	private String userId;
	private String firstName;
	private String lastName;
	private String email;
	private String role;
	private String status;
	
	public User() {
	}
	
	public User(com.okta.sdk.resource.user.User user) {
		this.userId = user.getId();
		this.firstName = user.getProfile().getFirstName();
		this.lastName = user.getProfile().getLastName();
		this.email = user.getProfile().getEmail();
		this.status = user.getStatus().name();
		
//		RoleList roleList = user.listRoles();
//		if (roleList != null) {
//			this.role = roleList.single().getType();
//		}
	}
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	

}
