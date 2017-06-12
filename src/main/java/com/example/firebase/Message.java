package com.example.firebase;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
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

    public static final String REQUEST_TITLE = "title";

    public static final String REQUEST_MESSAGE_TYPE = "messageType";

    public static final String REQUEST_BODY = "body";

    public static final String REQUEST_ID = "requestId";

    public static final String REQUEST_CLICK_ACTION = "clickAction";

    public static final String REQUEST_BUILD_INSTRUCTION = "build_instruction";

    public static final String REQUEST_BI_VALUE = "value";

    public static final String REQUEST_BI_LOOKUP = "lookup";

    public static final String REQUEST_BI_VALUE_OF = "valueOf";

    public static final String REQUEST_BI_REFERENCE = "reference";

    public static final String AGENT_TOKEN_SEPARATOR = "+";

    public static final String AGENT_TOKEN_SEPARATOR_REGEX = "\\+";

    public static final String MESSAGE_TYPE = "messageType";

    @Getter
    private String customerId;

    @Getter
    @Setter
    private JSONArray expansionInstruction;

    public Message(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Generates a push request with the title and message
     * 
     * @param title
     *            the title to be displayed
     * @param message
     *            the message to tbe displayed
     * @return
     * @throws JSONException
     */
    public static JSONObject newMessageRequest(String title, String messageType, String message) throws JSONException {
        JSONObject messageRequest = new JSONObject();

        messageRequest.put(REQUEST_TITLE, title);
        messageRequest.put(REQUEST_MESSAGE_TYPE, messageType);
        messageRequest.put(REQUEST_BODY, message);

        return messageRequest;
    }

    /**
     * Generates a push request with title, message and the action
     * 
     * @param title
     * @param message
     * @param action
     * @return
     * @throws JSONException
     */
    public static JSONObject newMessageRequest(String title, String messageType, String message, String action)
            throws JSONException {
        JSONObject messageRequest = new JSONObject();

        messageRequest.put(REQUEST_TITLE, title);
        messageRequest.put(REQUEST_BODY, message);
        messageRequest.put(MESSAGE_TYPE, messageType);
        messageRequest.put(REQUEST_CLICK_ACTION, action);

        return messageRequest;
    }

    public static JSONObject newMessageRequest(String action, String messageType) throws JSONException {
        JSONObject messageRequest = new JSONObject();

        messageRequest.put(REQUEST_CLICK_ACTION, action);
        messageRequest.put(MESSAGE_TYPE, messageType);

        return messageRequest;
    }

    public static void withStringExtra(JSONObject object, String key, String value) throws JSONException {
        object.put(key, value);
    }

    public static JSONObject withBuildInstruction(JSONObject message, JSONArray buildInstruction) throws JSONException {
        message.put(REQUEST_BUILD_INSTRUCTION, buildInstruction);
        return message;
    }

    /**
     * Convert this message to the appropriate request body. If this message is
     * supposed to be sent via HTTP as JSON, convert this message to the
     * required JSON format as a string.
     * 
     * @throws JSONException
     */
    public abstract String convertToRequestBody() throws JSONException;

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

    /**
     * Returns the target user agent of this message
     * 
     * @return the user agent
     */
    public abstract UserAgent getAgent();
}
