package com.example.firebase;

import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An abstract class that can be extended to implemented to various types of
 * message required by push messaging.
 * 
 * @author aanal
 *
 */
public abstract class Message {

    @Getter
    private String username;

    public Message(String username) {
        this.username = username;
    }

    public static void withStringExtra(JSONObject object, String key, String value) throws JSONException {
        object.put(key, value);
    }
    
    /**
     * The payload of the message
     * @return
     */
    abstract public String generateRequest();

    /**
     * A method to stop the thread when running
     * 
     * @return true if stop message, else not
     */
    public abstract boolean isStopMessage();

    /**
     * Get a list of recipients of this message
     * 
     * @return the list of ids or names
     */
    public abstract String[] getRecipientTokens();

}
