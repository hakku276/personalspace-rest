package com.example.firebase;

import java.io.IOException;
import java.util.HashMap;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;


/**
 * Firebase messaging service as per the docs
 * https://firebase.google.com/docs/cloud-messaging/server
 * https://firebase.google.com/docs/cloud-messaging/http-server-ref
 * 
 * @author aanal
 *
 */
public class FirebaseMessagingService extends HttpMessagingService {

    /**
     * A response handler interface for callbacks
     * 
     * @author aanal
     *
     */
    public interface FirebaseResponseHandler {

        /**
         * handle the response sent by the firebase server
         * 
         * @param message
         *            the requested message
         * @param status
         *            the response HTTP Status
         * @param results
         *            the results as per in docs
         */
        void handleResponse(Message message, HttpStatus status, JSONArray results);
    }

    /**
     * The standard google api url
     */
    private static final String url = "https://fcm.googleapis.com/fcm/send";

    /**
     * The standard google api method
     */
    private static final HttpMethod method = HttpMethod.POST;

    /**
     * The server key provided by google
     */
    private String serverKey;

    /**
     * The error handler
     */
    private FirebaseResponseHandler errorHandler;

    /**
     * The priority of the message
     * 
     * @author aanal
     *
     */
    public enum Priority {
        high("high"), normal("normal");

        @Getter
        private String value;

        private Priority(String value) {
            this.value = value;
        }
    }

    public FirebaseMessagingService(
            RestTemplate template,
            String serverKey,
            FirebaseResponseHandler firebaseErrorHandler) {
        super(template, url, method);
        this.serverKey = serverKey;
        this.errorHandler = firebaseErrorHandler;
    }

    @Override
    public void sendMessage(Message message) throws JSONException {
        HashMap<String, String> authorizationHeaders = new HashMap<String, String>();
        authorizationHeaders.put("Authorization", "key=" + serverKey);
        authorizationHeaders.put("Content-Type", "application/json");
        // make the request
        ResponseEntity<String> responseString = sendHttpMessage(authorizationHeaders, message);
        if (responseString != null) {
            JSONObject response = new JSONObject(responseString.getBody());
            errorHandler.handleResponse(message, responseString.getStatusCode(), response.getJSONArray("results"));
        }
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return false;
    }

}
