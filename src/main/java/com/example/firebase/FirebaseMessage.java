package com.example.firebase;

import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A firebase message class that can be sent to the firebase server
 * 
 * @author aanal
 *
 */
public class FirebaseMessage extends Message {

    private String to;

    @Getter
    private JSONObject payload;

    public FirebaseMessage(String username, JSONObject payload, String recipientToken) {
        super(username);
        this.to = recipientToken;
        this.payload = payload;
    }

    @Override
    public boolean isStopMessage() {
        return to.equals("STOP");
    }

    @Override
    public String[] getRecipientTokens() {
        return new String[] { to };
    }

    @Override
    public String generateRequest() {
        try{
            payload.put("to", to);
        } catch (JSONException e){
            return null;
        }
        return payload.toString();
    }
}
