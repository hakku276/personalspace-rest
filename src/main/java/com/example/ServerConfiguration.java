package com.example;

import com.example.firebase.FirebaseThreadedMessagingService;
import com.example.firebase.MessagingService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ServerConfiguration {

    private static final String SESSION_KEY = "app.session.passphrase";

    private static final String FCM_SERVER_KEY = "app.firebase.serverkey";

    @Getter
    private String sessionPass;

    @Getter
    private String serverKey;

    @Autowired
    public ServerConfiguration(Environment env) {
        sessionPass = env.getProperty(SESSION_KEY);
        serverKey = env.getProperty(FCM_SERVER_KEY);
        if (sessionPass == null || serverKey == null) {
            throw new IllegalStateException("No Configuration found");
        }
    }

    @Bean
    @Profile("prod")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Profile("prod")
    public MessagingService messagingService(RestTemplate restTemplate) {
        FirebaseThreadedMessagingService service = new FirebaseThreadedMessagingService(serverKey, restTemplate);
        service.start();
        return service;
    }
}
