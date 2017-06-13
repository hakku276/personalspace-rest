package com.example.firebase;

import org.json.JSONException;

public interface MessagingService {

    /**
     * Send the message to the destination
     * 
     * @param message
     *            the message to be sent
     * @throws JSONException
     */
    void sendMessage(Message message) throws JSONException;
}
