package io.reisys.uscis.fads.user.api.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.okta.sdk.client.Client;
import com.okta.sdk.resource.group.Group;
import com.okta.sdk.resource.group.GroupList;
import com.okta.sdk.resource.user.UserBuilder;
import com.okta.sdk.resource.user.UserList;
import com.okta.sdk.resource.user.UserStatus;

import io.reisys.uscis.fads.user.api.model.User;

@Component
public class OktaClientUtil {

    @Autowired
	private Client oktaClient;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OktaClientUtil.class);
    
    public List<User> getAllActiveUsers() {
    	UserList oktaUserList = oktaClient.listUsers();
    	if (oktaUserList != null ) {
    		LOGGER.info("Count of all users from Okta {}", oktaUserList.stream().count());
    		List<User> userList = new ArrayList<User>();
	    	for(com.okta.sdk.resource.user.User oktaUser: oktaUserList) {
	    		userList.add(createUserFromOktaUser(oktaUser));
	    	}
	    	return userList;
    	}
    	return null;
    }
    
    public List<User> getAllActiveUsersForGroup(String groupName) {
    	GroupList groupList = oktaClient.listGroups(groupName, null, null);
    	if (groupList != null) {
    		Group group = groupList.single();
    		UserList oktaUserList = group.listUsers();
    		if (oktaUserList != null ) {
        		List<User> userList = new ArrayList<User>();
    	    	for(com.okta.sdk.resource.user.User oktaUser: oktaUserList) {
    	    		if (UserStatus.ACTIVE.equals(oktaUser.getStatus())) {
	    	    		userList.add(createUserFromOktaUser(oktaUser));
    	    		}
    	    	}
        		LOGGER.info("Count of active users for group {} is {}", groupName, oktaUserList.stream().count());
    	    	return userList;
        	}
    	}

    	return null;
    }
    
    private User createUserFromOktaUser(com.okta.sdk.resource.user.User oktaUser) {
    	User user = new User();
    	user.setUserId(oktaUser.getId());
		user.setFirstName(oktaUser.getProfile().getFirstName());
		user.setLastName(oktaUser.getProfile().getLastName());
		user.setEmail(oktaUser.getProfile().getEmail());
		user.setStatus(oktaUser.getStatus().name());
		GroupList groupList = oktaUser.listGroups();
		if (groupList != null) {
			for (Group group: groupList) {
				if ("OKTA_GROUP".equals(group.getType())) {
					user.setRole(group.getProfile().getDescription());
					break;
				}
			}
		}
    	return user;
    }
    
    public User getUser(String userId) {
    	com.okta.sdk.resource.user.User oktaUser =  oktaClient.getUser(userId);
    	if (oktaUser != null) {
    		return createUserFromOktaUser(oktaUser);
    	}
    	return null;
    }
    
    public User createUser(User user, String groupName) {
    	com.okta.sdk.resource.user.User oktaUser = UserBuilder.instance()
    		    .setEmail(user.getEmail())
    		    .setFirstName(user.getFirstName())
    		    .setLastName(user.getLastName())
    		    .setPassword("Password".toCharArray())
    		    .setSecurityQuestion("Favorite security question?")
    		    .setSecurityQuestionAnswer("None of them!")
    		    .setActive(true)
    		    .buildAndCreate(oktaClient);
    	
    	GroupList groupList = oktaClient.listGroups(groupName, null, null);
    	if (groupList != null) {
    		Group group = groupList.single();
    		oktaUser.addToGroup(group.getId());
    	}
    	return createUserFromOktaUser(oktaUser);
    }
    
    public boolean deActivateUser(String userId) {
    	com.okta.sdk.resource.user.User oktaUser =  oktaClient.getUser(userId);
    	if (oktaUser != null) {
    		oktaUser.deactivate();
    		return true;
    	} 
    	return false;
    }
    
    public boolean updateUser(User user) {
    	com.okta.sdk.resource.user.User oktaUser =  oktaClient.getUser(user.getUserId());
    	if(oktaUser != null) {
    		oktaUser.getProfile().setFirstName(user.getFirstName());
    		oktaUser.getProfile().setLastName(user.getLastName());
    		oktaUser.getProfile().setEmail(user.getEmail());
    		oktaUser.update();
    		return true;
    	}
    	return false;
    }
}
