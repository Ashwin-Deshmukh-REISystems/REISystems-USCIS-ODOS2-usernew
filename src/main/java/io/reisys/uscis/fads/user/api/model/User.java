package io.reisys.uscis.fads.user.api.model;

import org.springframework.hateoas.ResourceSupport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class User  extends ResourceSupport {

	@ApiModelProperty(value="Id of user", required=true)
	private String userId;
	
	@ApiModelProperty(value="First Name for the user", required=true)
	private String firstName;
	
	@ApiModelProperty(value="Last Name for the user", required=true)
	private String lastName;
	
	@ApiModelProperty(value="Email for the user", required=true)
	private String email;
	
	@ApiModelProperty(value="Role name for the user", required=true)
	private String role;
	
	@ApiModelProperty(value="Status of the user", required=true)
	private String status;
	
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
