package io.reisys.uscis.odos.user.api.util;

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

import io.reisys.uscis.odos.user.api.model.User;

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
    	return new ArrayList<User>();
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

    	return new ArrayList<User>();
    }
    
    private User createUserFromOktaUser(com.okta.sdk.resource.user.User oktaUser) {
    	User user = new User();
    	user.setUserId(oktaUser.getId());
		user.setFirstName(oktaUser.getProfile().getFirstName());
		user.setLastName(oktaUser.getProfile().getLastName());
		user.setEmail(oktaUser.getProfile().getEmail());
		user.setStatus(oktaUser.getStatus().name());
		user.setRoles(new ArrayList<String>());
		GroupList groupList = oktaUser.listGroups();
		if (groupList != null) {
			for (Group group: groupList) {
				if ("OKTA_GROUP".equals(group.getType())) {
					user.getRoles().add(group.getProfile().getDescription());
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
    
    public boolean doesUserWithEmailExist(String email) {
    	UserList oktaUserList = oktaClient.listUsers(null, "profile.email eq \"" + email + "\"", null, null, null);
    	if (oktaUserList != null && oktaUserList.stream().count() > 0) {
    		return true;
    	} 
    	
    	return false;
    }
    
    public User createUser(User user) {
    	com.okta.sdk.resource.user.User oktaUser = UserBuilder.instance()
    		    .setEmail(user.getEmail())
    		    .setFirstName(user.getFirstName())
    		    .setLastName(user.getLastName())
    		    .setPassword("Pa$$w0rd".toCharArray())
    		    .setSecurityQuestion("Favorite security question?")
    		    .setSecurityQuestionAnswer("None of them!")
    		    .setActive(true)
    		    .buildAndCreate(oktaClient);
    	//Add roles
    	addRolesToUser(user, oktaUser);
    	return createUserFromOktaUser(oktaUser);
    }
    
    public boolean deActivateUser(String userId) {
    	com.okta.sdk.resource.user.User oktaUser =  oktaClient.getUser(userId);
    	if (oktaUser != null) {
    		LOGGER.info("Deactivating User");
    		oktaUser.deactivate();
    		return true;
    	} 
    	return false;
    }
    
    public boolean activateUser(String userId) {
    	com.okta.sdk.resource.user.User oktaUser =  oktaClient.getUser(userId);
    	if (oktaUser != null) {
    		LOGGER.info("Activating User");
    		oktaUser.activate(false);
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
    		
    		List<String> existingOktaRoles = new ArrayList<String>();
    		List<String> existingOktaRolesToRemove = new ArrayList<String>();
    		GroupList oktaUserGroupList = oktaUser.listGroups();
    		for (Group oktaUserGroup : oktaUserGroupList) {
    			String roleName = oktaUserGroup.getProfile().getDescription();
    			if (!user.getRoles().contains(roleName)) {
    				LOGGER.info("Role {} needs to be removed", roleName);
    				existingOktaRolesToRemove.add(roleName);
    			}
    			existingOktaRoles.add(oktaUserGroup.getProfile().getDescription());
    		}
    		
    		user.getRoles().removeAll(existingOktaRoles);
    		//Add roles
    		addRolesToUser(user, oktaUser);
        	
    		//Remove roles
        	removeRolesForUser(user, existingOktaRolesToRemove);
        	
    		oktaUser.update();
    		return true;
    	}
    	return false;
    }

	private void removeRolesForUser(User user, List<String> existingOktaRolesToRemove) {
		
		for (String roleToRemove: existingOktaRolesToRemove) {
			LOGGER.info("Removing Role {} for user", roleToRemove);
			GroupList groupList = oktaClient.listGroups(roleToRemove, null, null);
			if (groupList != null && groupList.stream().count() == 1) {
				Group group = groupList.single();
				group.removeUser(user.getUserId());
			}
		}
	}

	private void addRolesToUser(User user, com.okta.sdk.resource.user.User oktaUser) {
		
		for (String role : user.getRoles() ) {
			LOGGER.info("Role {} needs to be added", role);
			GroupList groupList = oktaClient.listGroups(role, null, null);
			if (groupList != null) {
				Group group = groupList.single();
				oktaUser.addToGroup(group.getId());
			}
		}
	}
}
