package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class PersonalSpaceApplication {

    @RequestMapping(value = "/sessions", method = RequestMethod.GET)
    public String greet(){
        return "Hello World!!";
    }
    
    
	public static void main(String[] args) {
		SpringApplication.run(PersonalSpaceApplication.class, args);
	}
}
