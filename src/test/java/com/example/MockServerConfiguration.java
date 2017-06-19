package com.example;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.firebase.MessagingService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(basePackageClasses = { PersonalSpaceApplication.class })
public class MockServerConfiguration {

    @Bean
    @Profile("unit")
    public RestTemplate getMockRestTemplate() {
        RestTemplate template = new RestTemplate();

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(template);
        // no easy way found till now Date: 12 May 2016
        // using solution as suggested in:
        // http://stackoverflow.com/questions/30713734/spring-mockrestserviceserver-handling-multiple-requests-to-the-same-uri-auto-di

        // mocks 40 calls to the server
        // hopefully this is enough
        for (int i = 0; i < 40; i++) {
            mockServer.expect(anything())
                    .andRespond(withSuccess());
        }

        return template;
    }

    @Bean
    @Profile("unit")
    public MessagingService messagingService() {
        return Mockito.mock(MessagingService.class);
    }
}
