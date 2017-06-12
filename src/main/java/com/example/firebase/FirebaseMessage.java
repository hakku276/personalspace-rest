package com.example.firebase;

import com.example.firebase.FirebaseMessagingService.Priority;
import java.util.Arrays;
import lombok.Getter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A firebase message class that can be sent to the firebase server
 * 
 * @author aanal
 *
 */
public class FirebaseMessage extends Message {

    /**
     * The types of message available for firebase
     * 
     * @author aanal
     *
     */
    private enum Type {
        notification("notification"), data("data"), unknown("unknown");

        @Getter
        private String value;

        Type(String value) {
            this.value = value;
        }

        public static Type parse(String value) {
            for (Type eType : Type.values()) {
                if (eType.value.equals(value)) {
                    return eType;
                }
            }
            return Type.unknown;
        }
    }

    /**
     * the recipient unique id, or a topic
     */
    private String to;

    /**
     * The type of message to be pushed
     */
    private Type messageType;

    /**
     * the ids of the recipients of a topic
     */
    private String[] registrationIds;

    /**
     * The collapse message key
     */
    private String collapseKey;

    /**
     * The defined priority
     */
    private Priority priority;

    /**
     * Only for iOS to wake up sleeping app
     */
    private boolean contentAvailable;

    /**
     * should the server open a connection to send the message while the phone
     * is idle
     */
    private boolean delayWhileIdle;

    /**
     * the amount of time the server stores the message
     */
    private long timeToLive;

    /**
     * the package which will receive the message
     */
    private String restrictedPackageName;

    /**
     * denotes whether this message is for test purposes
     */
    private boolean dryRun;

    /**
     * the payload as data which is sent to the client app for processing
     */
    @Getter
    private JSONObject data;

    private Logger logger = LogManager.getLogger(FirebaseMessage.class);

    public FirebaseMessage(String customerId, String messageType, String recipientToken) {
        super(customerId);
        this.to = recipientToken;
        this.messageType = Type.parse(messageType);
        priority = Priority.normal;
        this.data = new JSONObject();
    }

    @Override
    public String convertToRequestBody() throws JSONException {
        JSONObject requestBody = new JSONObject();

        // split the user agent and the token
        String[] parts = to.split(Message.AGENT_TOKEN_SEPARATOR_REGEX);
        if (parts.length != 2) {
            logger.error("The message has been split unexpectedly : " + Arrays.toString(parts));
            return null;
        }

        // split was even, verify agent type
        UserAgent agent = UserAgent.parse(parts[0]);
        if (agent == UserAgent.UNKNOWN) {
            logger.error("The agent was unknown: " + parts[0]);
            return null;
        }

        // get the actual token
        String recipient = parts[1];
        requestBody.put("to", recipient);

        // check the message type
        if (messageType == Type.unknown) {
            logger.error("Unknown message type requested: " + messageType);
            return null;
        }

        // determine where to put the message
        if (messageType == Type.notification && agent == UserAgent.ANDROID) {
            // notification in android is put in data message
            requestBody.put(Type.data.getValue(), data);
        } else if (messageType == Type.notification && agent == UserAgent.IOS) {
            // notification in iOS is put in notification message
            requestBody.put(Type.notification.getValue(), data);
        } else if (messageType == Type.data) {
            // data type in both device are put in data message
            requestBody.put(Type.data.getValue(), data);
        } else {
            logger.error(
                    "The message type and agent combination is not known: (agent= " +
                            agent +
                            ", type=" +
                            messageType +
                            ")");
            return null;
        }

        // add the registration ids if present
        JSONArray regIds = new JSONArray();
        if (registrationIds != null && registrationIds.length > 0) {
            for (String string : registrationIds) {
                regIds.put(string);
            }
            requestBody.put("registration_ids", regIds);
        }

        // set the other parameters
        if (collapseKey != null && collapseKey.length() > 0) {
            requestBody.put("collapse_key", collapseKey);
        }
        requestBody.put("priority", priority.getValue());
        if (contentAvailable != false) {
            // default value is false
            requestBody.put("content_available", contentAvailable);
        }
        if (delayWhileIdle != false) {
            // default value is false
            requestBody.put("delay_while_idle", delayWhileIdle);
        }
        if (timeToLive > 0) {
            requestBody.put("time_to_live", timeToLive);
        }
        if (restrictedPackageName != null && restrictedPackageName.length() > 0) {
            requestBody.put("restricted_package_name", restrictedPackageName);
        }
        if (dryRun != false) {
            // default value is false
            requestBody.put("dry_run", dryRun);
        }

        return requestBody.toString();
    }

    @Override
    public String[] getRecipientTokens() {
        if (registrationIds == null || registrationIds.length == 0) {
            return new String[] { to };
        }
        return registrationIds;
    }

    @Override
    public boolean isStopMessage() {
        return (getCustomerId().equals("stop") && messageType == Type.unknown && to.equals("stop"));
    }

    @Override
    public UserAgent getAgent() {
        String[] parts = to.split(Message.AGENT_TOKEN_SEPARATOR_REGEX);
        if (parts.length != 2) {
            logger.error("Improportional split: " + Arrays.toString(parts));
            return UserAgent.UNKNOWN;
        }
        return UserAgent.parse(parts[0]);
    }

    public FirebaseMessage withRegistrationIds(String[] registrationIds) {
        this.registrationIds = registrationIds;
        return this;
    }

    public FirebaseMessage withCollapseKey(String key) {
        this.collapseKey = key;
        return this;
    }

    public FirebaseMessage withPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public FirebaseMessage isContentAvailable(boolean available) {
        this.contentAvailable = available;
        return this;
    }

    public FirebaseMessage isDelayedWhileIdle(boolean delay) {
        this.delayWhileIdle = delay;
        return this;
    }

    public FirebaseMessage withTimeToLive(long ttl) {
        this.timeToLive = ttl;
        return this;
    }

    public FirebaseMessage withRestrictedPackageName(String packageName) {
        this.restrictedPackageName = packageName;
        return this;
    }

    public FirebaseMessage isTestRun(boolean test) {
        this.dryRun = test;
        return this;
    }

    public FirebaseMessage withDataEntry(String key, String value) throws JSONException {
        data.put(key, value);
        return this;
    }

    public FirebaseMessage withData(JSONObject data) {
        this.data = data;
        return this;
    }
}
