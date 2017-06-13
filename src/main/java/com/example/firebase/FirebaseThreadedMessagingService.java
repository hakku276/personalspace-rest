package com.example.firebase;

import com.example.firebase.FirebaseMessagingService.FirebaseResponseHandler;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

public class FirebaseThreadedMessagingService extends Thread implements MessagingService, FirebaseResponseHandler {

    /**
     * The delay generator that is used to create random delay
     */
    private Random delayGenerator;

    /**
     * The logger to log any errors
     */
    private Logger logger = LogManager.getLogger(FirebaseThreadedMessagingService.class);

    /**
     * A timer used to delay any subsequent tasks.
     */
    private Timer delayScheduler;

    /**
     * The blocking Firebase messaging service
     */
    private FirebaseMessagingService messagingService;

    /**
     * The message queue
     */
    private BlockingQueue<Message> messageQueue;

    /**
     * The template used to access HTTP messaging
     */
    private RestTemplate restTemplate;

    /**
     * Useful when testing this threaded service
     * 
     * @param serverKey
     * @param template
     */
    public FirebaseThreadedMessagingService(String serverKey, RestTemplate template) {
        this.restTemplate = template;
        messagingService = new FirebaseMessagingService(restTemplate, serverKey, this);
        messageQueue = new LinkedBlockingQueue<Message>();
        delayScheduler = new Timer();
        delayGenerator = new Random(new Date().getTime());
    }

    @Override
    public void run() {
        logger.debug("Starting Messaging service: ");
        while (true) {
            try {
                Message message = messageQueue.take();
                if (message.isStopMessage()) {
                    logger.warn("Stopping message service");
                    break;
                } else {
                    if (message instanceof FirebaseMessage) {
                        messagingService.sendMessage(message);
                    } else {
                        logger.error("Trying to send a non firebase message to a firebase server");
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("The thread was interrupted while waiting for message queue");
                logger.warn(e.getStackTrace()
                        .toString());
            } catch (JSONException e) {
                logger.error("Could not parse JSON");
                logger.error(Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public void sendMessage(Message message) {
        if (!messageQueue.offer(message)) {
            logger.error("The message could not be send because the queue was full");
            logger.error(message.generateRequest());
        }
    }

    /**
     * Sends a stop message to the thread, useful in testing conditions, for the
     * thread to stop
     */
    public void sendStopMessage() {
        // A default stop type message
        FirebaseMessage stopMessage = new FirebaseMessage("STOP", null, "STOP");
        if (!messageQueue.offer(stopMessage)) {
            logger.error("The stop message could not be sent");
        }
    }

    /**
     * Handle the response sent by the FCM server
     */
    public void handleResponse(final Message message, HttpStatus status, JSONArray results) {
        // check for the simplest mistake
        if (status == HttpStatus.BAD_REQUEST) {
            // the request json was malformed
            logger.error("The JSON message was ill-formed");
            logger.error("Request: " + message.generateRequest());
            return;
        } else if (status == HttpStatus.UNAUTHORIZED) {
            logger.error("Server Key was incorrect.");
            return;
        }

        String[] registrationIds = message.getRecipientTokens();
        if (registrationIds == null) {
            logger.error("The registration ids were null for customer: " + message.getUsername());
            logger.error("With message: " + message.generateRequest());
            return;
        }

        if (registrationIds.length != results.length()) {
            // The mismatch between the request and the response
            logger.error("Mismatched request and response");
            logger.error("Request: " + message.generateRequest());
            logger.error("Response: " + results.toString());
            return;
        }

        // everything okay, start processing the response
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                if (result.has("error")) {
                    // the result has an error
                    String error = result.getString("error");
                    if (status == HttpStatus.OK) {
                        if (error.equals("MissingRegistration")) {
                            logger.error("The request was missing registration id");
                            logger.error(message.generateRequest());
                        } else if (error.equals("InvalidRegistration")) {
                            // the registration id sent was invalid, may be due
                            // to
                            // added characters
                            logger.error("The registration token was invalid:");
                            logger.error(
                                    "Customer ID: " + message.getUsername() + " Message: " + message.generateRequest());
                            logger.error("Removed the token from the database");
                            removeToken(message.getUsername(), registrationIds[i]);
                        } else if (error.equals("NotRegistered")) {
                            // existing token is invalid due to
                            // 1. app unregistered with fcm
                            // 2. uninstalled application
                            // 3. token expires
                            // 4. app updated but the updated app is not
                            // configured
                            // to receive messages
                            // remove the token from the database
                            removeToken(message.getUsername(), registrationIds[i]);
                        } else if (error.equals("InvalidPackageName")) {
                            // the package name was invalid
                            logger.error("The package name was found to be invalid");
                            logger.error(message.generateRequest());
                        } else if (error.equals("MismatchSenderId")) {
                            // the sender id was invalid to send messages
                            logger.error("The Sender Id was invalid");
                            logger.error(message.generateRequest());
                        } else if (error.equals("MessageTooBig")) {
                            logger.error("Message Body found to be too big");
                        } else if (error.equals("InvalidTtl")) {
                            logger.error("TTL was invalid");
                            logger.error(message.generateRequest());
                        } else if (error.equals("InvalidDataKey")) {
                            logger.error("The message contains invalid keys");
                            logger.error(message.generateRequest());
                        } else if (error.equals("DeviceMessageRateExceeded")) {
                            // the device message rate has been exceeded
                        } else if (error.equals("TopicsMessageRateExceeded")) {
                            // the topic has exceeded its message rate
                            logger.error("The Topic has exceeded its message rate");
                        }
                    }
                    // these errors are common to multiple types of statuses
                    if (error.equals("Unavailable") || error.equals("InternalServerError")) {
                        // TODO retry exponentially
                        delayScheduler.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                sendMessage(message);

                            }
                        }, delayGenerator.nextInt(50000) + 10000);
                        // delay of random to 10000 to 50000 milli seconds
                    }
                } else if (result.has("registration_id")) {
                    // the message sent was successful but need to update the
                    // key
                    replaceToken(message.getUsername(), result.getString("registration_id"), registrationIds[i]);
                }
            }
        } catch (JSONException e) {
            // log error could not parse the result
            logger.error("The response message could not be parsed: ");
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Remove the specified token from the customer table
     * 
     * @param customerId
     *            the customer id whose token is to be removed
     * @param token
     *            the token to be removed
     */
    private void removeToken(String customerId, String token) {
        // TODO remove the token
    }

    /**
     * Replace the token present in the database with the returned value
     * 
     * @param customerId
     *            the customer reference
     * @param agent
     *            the agent, used to store token in database
     * @param token
     *            the token present in the database
     * @param tokenToReplace
     *            the actual token returned by the fcm server
     */
    private void replaceToken(String customerId, String token, String tokenToReplace) {
        // TODO replace the token
    }
}
