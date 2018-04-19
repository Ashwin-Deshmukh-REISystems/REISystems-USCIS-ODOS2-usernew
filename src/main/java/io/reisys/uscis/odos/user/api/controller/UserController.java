package io.reisys.uscis.odos.user.api.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import io.reisys.uscis.odos.user.api.model.User;
import io.reisys.uscis.odos.user.api.util.AccessManagementUtil;
import io.reisys.uscis.odos.user.api.util.OktaClientUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/api/v1/user")
@Api(tags = "User")
public class UserController {
	
    @Autowired
	private OktaClientUtil oktaClientUtil;
    
    @Autowired
    private AccessManagementUtil accessManagementUtil;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    @RequestMapping(method = RequestMethod.GET, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Get list of Users", notes=" Get list of Users")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful retrieval",
	            response = io.reisys.uscis.odos.user.api.model.User.class, responseContainer = "List") })    
    public ResponseEntity<Resources<User>>  getUsers(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken
    		) {

    	LOGGER.info("Retrieving list of Users");
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	List<User> userList = oktaClientUtil.getAllActiveUsers();
    	if (userList != null ) {
	    	for(User user: userList) {
	    		assembleLinks(accessToken, user);
	    	}
	    	
	    	List<Link> links = new ArrayList<>();
		    UserController builder = methodOn(UserController.class);
		
	        // self
	        links.add(linkTo(builder.getUsers(accessToken)).withSelfRel());
	
	        // search
	        ControllerLinkBuilder searchLinkBuilder = linkTo(builder.getUsers(accessToken));
	        Link searchLink = new Link(searchLinkBuilder.toString() , "search");
	        links.add(searchLink);
	
	        return ResponseEntity.ok().body(new Resources<>(userList, links));
    	} else {
    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    	}
    	
    }
    
    @RequestMapping(value = "/{userId}", method = RequestMethod.GET, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Get User Information")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	        @ApiResponse(code = 200, message = "Successful retrieval",
	            response = User.class) })      
    public ResponseEntity<User> getUser(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken,
    		@ApiParam(value = "The id of the user being retrieved", required = true) @PathVariable("userId") String userId) {
    	LOGGER.info("Retrieving User with id {}", userId);
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	
    	User user = oktaClientUtil.getUser(userId);
    	if (user != null) {
    		LOGGER.info("User exists ");
    		assembleLinks(accessToken, user);
        	return ResponseEntity.ok().body(user);
    	} else {
    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    	}
        
    }
    
    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Create User Information")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful retrieval",
       response = User.class) })    
    public ResponseEntity<User> createUser(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken,
    		@ApiParam(value = "The json for User being created", required = true) @RequestBody String jsonData) {

    	LOGGER.info("Creating User");
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	Gson gson = new Gson();
        User userFromJson = gson.fromJson(jsonData, User.class);
        
        if (userFromJson.getEmail() != null && !oktaClientUtil.doesUserWithEmailExist(userFromJson.getEmail())) {
        	User user = oktaClientUtil.createUser(userFromJson);
        	return ResponseEntity.ok().body(user);
        } else {
        	userFromJson.setErrorMessages(new ArrayList<String>());
        	userFromJson.getErrorMessages().add("User with email already exists");
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userFromJson);
        }
        
    }
    
    
    @RequestMapping(value = "/{userId}/update", method = RequestMethod.PATCH, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Save User Information")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful retrieval",
       response = User.class) })    
    public ResponseEntity<User> updateUser(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken,
    		@ApiParam(value = "The id of the user being retrieved", required = true) @PathVariable("userId") String userId,
    			@ApiParam(value = "The json for User being updated", required = true) @RequestBody String jsonData) {

    	LOGGER.info("Updating User with id {}", userId);
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	
    	Gson gson = new Gson();
    	User userFromJson = gson.fromJson(jsonData, User.class);
    	boolean updateSuccessful = oktaClientUtil.updateUser(userFromJson);
    	if (updateSuccessful) {
    		User savedUser = oktaClientUtil.getUser(userId);
    		return ResponseEntity.ok().body(savedUser);
    	} else {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    @RequestMapping(value = "/{userId}/delete", method = RequestMethod.DELETE, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Delete User Information")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful Delete",
       response = User.class) })    
    public ResponseEntity<Boolean> deleteUser(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken,
    		@ApiParam(value = "The id of the user being deleted", required = true) @PathVariable("userId") String userId) {
    	LOGGER.info("Deleting User with id {}", userId);
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	
    	boolean deleteSuccessful = oktaClientUtil.deActivateUser(userId);

        if (deleteSuccessful) {
			return ResponseEntity.ok().body(Boolean.TRUE);
        } else {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Boolean.FALSE);
        }
    }
    
    @RequestMapping(value = "/{userId}/activate", method = RequestMethod.DELETE, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Activate User Information")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful Delete",
       response = User.class) })    
    public ResponseEntity<Boolean> activateUser(
    		@ApiParam(value = "The access token to validate", required = true) @RequestHeader(value = "X-Auth-Token", required = true) String accessToken,
    		@ApiParam(value = "The id of the user being activated", required = true) @PathVariable("userId") String userId) {
    	LOGGER.info("Deleting User with id {}", userId);
    	String userIdFromToken = accessManagementUtil.getUserIdFromToken(accessToken);
    	if (userIdFromToken == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    	}
    	
    	boolean deleteSuccessful = oktaClientUtil.activateUser(userId);

        if (deleteSuccessful) {
			return ResponseEntity.ok().body(Boolean.TRUE);
        } else {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Boolean.FALSE);
        }
    }
    
    
    protected void assembleLinks(String accessToken, User user) {
    	user.add(linkTo(methodOn(UserController.class).getUser(accessToken, user.getUserId())).withSelfRel());
    }
}
