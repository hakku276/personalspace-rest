package com.example.firebase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface MessagingService {

    /**
     * Send the message to the destination
     * 
     * @param message
     *            the message to be sent
     * @throws JSONException
     */
    void sendMessage(Message message) throws JSONException;

    /**
     * Send the message to the destination
     * 
     * @param message
     *            the json message received via network
     * @param customerId
     *            the customer id receiving the message
     * @param token
     *            the specific device token receiving this message
     * @throws JSONException
     */
    void sendMessage(String messageType, String customerId, String token, JSONObject message,
            JSONArray buildInstruction) throws JSONException;
}
