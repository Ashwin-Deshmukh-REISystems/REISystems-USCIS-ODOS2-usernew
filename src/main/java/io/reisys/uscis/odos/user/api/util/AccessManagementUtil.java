package io.reisys.uscis.odos.user.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component
public class AccessManagementUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccessManagementUtil.class);
	
	@Autowired
	private Environment environment;
	
	private JsonObject getJsonDataFromAPI(String api,  MultiValueMap<String, String> paramMap) {
		JsonObject json = null;
		
	    String authorizationEndPoint = String.format("%s"+api, environment.getProperty("OKTA_CLIENT_ORGURL"));
	    
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	    
	    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(paramMap, headers);
	     
	    UriComponentsBuilder authorizationUriBuilder = UriComponentsBuilder.fromHttpUrl(authorizationEndPoint);
	    
	    LOGGER.info("Authorization Call URI is35 {}", authorizationUriBuilder.build().encode().toUri());
	    
	    RestTemplate restTemplate = new RestTemplate();
	    ResponseEntity<String> result = restTemplate.postForEntity(authorizationUriBuilder.build().encode().toUri(), requestEntity, String.class );
	    		
	    LOGGER.info("API Call status is {}", result.getStatusCodeValue());
	    if (result.getStatusCode() == HttpStatus.OK) {
	    	 Gson gson =  new Gson();
	    	 json = gson.fromJson(result.getBody(), JsonObject.class);
	    }
		
	    return json;
	}
	
	public String getUserIdFromToken(String accessToken) {
	    MultiValueMap<String, String> paramMap= new LinkedMultiValueMap<String, String>();
	    LOGGER.info("Client id is "+ environment.getProperty("OKTA_CLIENT_ID") );
	    paramMap.add("client_id", environment.getProperty("OKTA_CLIENT_ID"));
	    paramMap.add("token", accessToken);
	    
		JsonObject jsonFromOkta = getJsonDataFromAPI("/oauth2/default/v1/introspect", paramMap);
		boolean isTokenActive =  jsonFromOkta.get("active").getAsBoolean();
		if (isTokenActive) {
			return jsonFromOkta.get("sub").getAsString();
		}
		return null;
	}
	

}
