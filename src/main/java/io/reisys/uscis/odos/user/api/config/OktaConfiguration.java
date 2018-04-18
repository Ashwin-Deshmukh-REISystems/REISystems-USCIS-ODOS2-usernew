package io.reisys.uscis.odos.user.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;

@Configuration
public class OktaConfiguration {
	
	@Autowired
    private Environment environment;

	@Bean
	public Client oktaClient() {
		
		Client client = Clients.builder()
		        .setClientCredentials(new TokenClientCredentials(environment.getProperty("OKTA_CLIENT_TOKEN")))
		        .setOrgUrl(environment.getProperty("OKTA_CLIENT_ORGURL"))
		        .build();
		return client;
	}
}
