package com.example.firebase;

import lombok.Getter;

/**
 * Various types of agents receiving notifications
 * 
 * @author aanal
 *
 */
public enum UserAgent {
    ANDROID("ANDROID"), IOS("IOS"), WEB("WEB"), UNKNOWN("UNKNOWN");

    @Getter
    private String value;

    UserAgent(String value) {
        this.value = value;
    }

    public static UserAgent parse(String userAgent) {
        userAgent = userAgent.toLowerCase();

        for (UserAgent agent : UserAgent.values()) {
            if (userAgent.contains(agent.value.toLowerCase())) {
                return agent;
            }
        }

        // TODO test also for browser based web services

        return UserAgent.UNKNOWN;
    }

}
