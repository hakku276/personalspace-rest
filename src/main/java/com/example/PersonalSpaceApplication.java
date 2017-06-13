package com.example;

import com.example.firebase.FirebaseMessage;
import com.example.firebase.MessagingService;
import com.example.personalspace.Preference;
import com.example.personalspace.Session;
import com.example.personalspace.User;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class PersonalSpaceApplication {

    private static final String SESSION_PASS_KEY = "passkey";

    private static final String SESSION_NAME = "name";

    private static final String SESSION_STATUS = "status";

    private static final String MEDIA_TYPE = "application/json";

    @Autowired
    private ServerConfiguration config;

    @Autowired
    private MessagingService messagingService;

    private Logger logger = LogManager.getLogger(PersonalSpaceApplication.class);

    @Getter
    private Session session;

    @RequestMapping(value = "/sessions", method = RequestMethod.POST, consumes = MEDIA_TYPE, produces = MEDIA_TYPE)
    public ResponseEntity<String> sessionOperation(@RequestBody Map<String, String> request) throws JSONException {
        if (!request.containsKey(SESSION_STATUS)) {
            logger.error("Session Requested Status not available");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Status for the session not specified");
            return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
        }

        Session.Status requestedStatus = Session.Status.valueOf(request.get(SESSION_STATUS));

        if (session != null &&
                session.getStatus() == Session.Status.ACTIVE &&
                requestedStatus == Session.Status.ACTIVE) {
            // session already active, but new session wanted to be created
            logger.warn("A session is already active, cannot handle multiple sessions");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.OK.value());
            json.put("message", "Session Already Started");
            json.put("session", session.toJSON());
            return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
        }

        if (!request.containsKey(SESSION_PASS_KEY) || !request.get(SESSION_PASS_KEY)
                .equals(config.getSessionPass())) {
            logger.error("The Password was not specified or incorrect to start a session");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.UNAUTHORIZED.value());
            json.put("message", "passkey field not set or incorrect");
            return new ResponseEntity<String>(json.toString(), HttpStatus.UNAUTHORIZED);
        }

        if (!request.containsKey(SESSION_NAME)) {
            logger.error("The name for the session has not been specified to start a session");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Name for the session not specified");
            return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
        }

        // create a new Session
        if (requestedStatus == Session.Status.ACTIVE) {
            session = new Session();

            logger.info("New Session Created");

            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.CREATED.value());
            json.put("message", "New Session Created");
            json.put("session", session.toJSON());

            return new ResponseEntity<String>(json.toString(), HttpStatus.CREATED);
        } else if (requestedStatus == Session.Status.INACTIVE) {
            if (session != null) {
                session.setStatus(Session.Status.INACTIVE);
                session.setEndDate(new Date());
            }

            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.OK.value());
            json.put("message", "Session Closed");
            if (session != null) {
                json.put("session", session.toJSON());
            }

            return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
        }

        logger.error("Invalid Request");
        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.BAD_REQUEST.value());
        json.put("message", "Unknown Invalid Request");
        return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/sessions/{sessionId}", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public ResponseEntity<Session> getActiveSession(@PathVariable(value = "sessionId") long sessionId)
            throws JSONException {
        if (session == null || session.getStatus() == Session.Status.INACTIVE) {
            logger.error("No Session Available");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.NO_CONTENT.value());
            json.put("message", "No session Available");
            return new ResponseEntity<Session>(HttpStatus.NO_CONTENT);
        }
        if (sessionId == session.getId()) {
            return new ResponseEntity<Session>(session, HttpStatus.OK);
        }
        return new ResponseEntity<Session>(HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(value = "/sessions/users", method = RequestMethod.POST, consumes = MEDIA_TYPE, produces = MEDIA_TYPE)
    public ResponseEntity<String> addUserToSession(@RequestBody Map<String, Object> request) throws JSONException {
        if (session == null || session.getStatus() == Session.Status.INACTIVE) {
            // the session has not started to add any user to it
            logger.error("Session Not Started Yet to add users");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Session not initiated, Please Start a session before adding user");
            return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.fromMap(request);

        if (!session.addUser(user)) {
            logger.error("User already added");
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "User already added");
            return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
        }

        logger.info(String.format("%s added to the list of users successfully", user.getName()));

        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.OK.value());
        json.put("message", "User added successfully");
        return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/sessions/users/{name}", method = RequestMethod.DELETE, produces = MEDIA_TYPE)
    public ResponseEntity<String> removeUserFromSession(@PathVariable(value = "name") String name)
            throws JSONException, UnsupportedEncodingException {
        String username = URLDecoder.decode(name, "utf-8");
        logger.info("Removing " + username);
        session.removeUser(username);

        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.OK.value());
        json.put("message", "User removed");
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sessions/users/{name}", method = RequestMethod.PUT, consumes = MEDIA_TYPE, produces = MEDIA_TYPE)
    public ResponseEntity<String> updatePreference(@PathVariable(value = "name") String name,
            @RequestBody Map<String, Object> request) throws JSONException, UnsupportedEncodingException {
        String username = URLDecoder.decode(name, "utf-8");

        if (!request.containsKey("pref")) {
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Incomplete Request");
            return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
        }

        Preference pref = new Preference();
        pref.fromMap((Map<String, Object>) request.get("pref"));

        session.updatePreference(username, pref);

        JSONObject json = new JSONObject();
        json.put("status", HttpStatus.OK.value());
        json.put("message", "User spec updated successfully");
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value = "/sessions/users", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public ResponseEntity<String[]> getAllCustomers() {
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // get the customers
        String[] users = session.getActiveUsers()
                .keySet()
                .toArray(new String[session.getActiveUsers()
                        .size()]);

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @RequestMapping(value = "/sessions/users/{name}/notify")
    public ResponseEntity<String> notifyUser(@PathVariable(value = "name") String name,
            @RequestBody Map<String, Object> request) throws JSONException, UnsupportedEncodingException {

        String username = URLDecoder.decode(name, "utf-8");
        if (session == null || session.getStatus() == Session.Status.INACTIVE) {
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.UNAUTHORIZED.value());
            json.put("message", "Session already closed");
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }

        if (!request.containsKey(SESSION_PASS_KEY) || !request.containsKey("message")) {
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "The Request does not contain passkey and or message");
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }

        // check if the pass key is correct or not
        String passKey = (String) request.get(SESSION_PASS_KEY);
        if (!passKey.equals(config.getSessionPass())) {
            // the session pass is incorrect
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.UNAUTHORIZED.value());
            json.put("message", "Could not verify the security key");
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }

        // put through the message
        Gson gson = new Gson();
        String str = gson.toJson(request.get("message"));
        if (str == null) {
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Could not parse the message payload");
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
        JSONObject payload = new JSONObject(str);

        // get the token of the user
        User user = session.getActiveUsers()
                .get(username);

        if (user == null) {
            JSONObject json = new JSONObject();
            json.put("status", HttpStatus.BAD_REQUEST.value());
            json.put("message", "Could not verify the security key");
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }

        FirebaseMessage message = new FirebaseMessage(username, payload, user.getPushToken());

        messagingService.sendMessage(message);

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    public static void main(String[] args) {
        SpringApplication.run(PersonalSpaceApplication.class, args);
    }
}
