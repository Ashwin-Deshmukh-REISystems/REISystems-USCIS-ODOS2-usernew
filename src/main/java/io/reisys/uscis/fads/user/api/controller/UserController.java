package io.reisys.uscis.fads.user.api.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.okta.sdk.client.Client;
import com.okta.sdk.resource.user.UserList;

import io.reisys.uscis.fads.user.api.model.User;
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
	private Client oktaClient;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    @RequestMapping(method = RequestMethod.GET, produces = MediaTypes.HAL_JSON_VALUE)
    @ApiOperation(value = "Get list of Users", notes=" Get list of Users")
    @ApiResponses(value = {
	         @ApiResponse(code = 404, message = "Service not found"),
	         @ApiResponse(code = 200, message = "Successful retrieval",
	            response = io.reisys.uscis.fads.user.api.model.User.class, responseContainer = "List") })    
    public ResponseEntity<Resources<User>>  getUsers() {

    	LOGGER.info("Retrieving list of Users");
    	
    	UserList oktaUserList = oktaClient.listUsers();
    	if (oktaUserList != null ) {
    		List<User> userList = new ArrayList<User>();
	    	for(com.okta.sdk.resource.user.User oktaUser: oktaUserList) {
	    		User user = new User(oktaUser);
	    		assembleLinks(user);
	    		userList.add(user);
	    	}
	    	
	    	List<Link> links = new ArrayList<>();
		    UserController builder = methodOn(UserController.class);
		
	        // self
	        links.add(linkTo(builder.getUsers()).withSelfRel());
	
	        // search
	        ControllerLinkBuilder searchLinkBuilder = linkTo(builder.getUsers());
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
    public ResponseEntity<User> getUser(@ApiParam(value = "The id of the user being retrieved", required = true) @PathVariable("userId") String userId) {
    	LOGGER.info("Retrieving User with id {}", userId);
    	
    	com.okta.sdk.resource.user.User oktaUser = oktaClient.getUser(userId);
    	if (oktaUser != null) {
    		LOGGER.info("User exists ");
    		User user = new User(oktaUser);
    		assembleLinks(user);
        	return ResponseEntity.ok().body(user);
    	} else {
    		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    	}
        
    }
    
    
    protected void assembleLinks(User user) {
    	user.add(linkTo(methodOn(UserController.class).getUser(user.getUserId())).withSelfRel());
    }
}
