package com.example;

import com.example.firebase.FirebaseThreadedMessagingService;
import com.example.firebase.MessagingService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

@Configuration
@PropertySource("classpath:config.properties")
public class ServerConfiguration {
    
    @Value("${app.session.passphrase}")
    @Getter
    private String sessionPass;
    
    @Value("${app.firebase.serverkey}")
    private String serverKey;
    
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
    
    @Bean
    public MessagingService messagingService(RestTemplate restTemplate){
        FirebaseThreadedMessagingService service = new FirebaseThreadedMessagingService(serverKey, restTemplate);
        service.start();
        return service;
    }
    
    @Bean
    static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
