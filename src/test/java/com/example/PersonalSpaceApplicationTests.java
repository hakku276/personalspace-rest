package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.example.firebase.MessagingService;
import com.example.personalspace.User;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { MockServletContext.class, MockServerConfiguration.class })
@ActiveProfiles("unit")
@WebAppConfiguration
public class PersonalSpaceApplicationTests {

    static {
        System.out.println("Setting KEYS");
        System.setProperty("app.firebase.serverkey", "foo");
        System.setProperty("app.session.passphrase", "foo");
    }

    @Autowired
    private PersonalSpaceApplication controller;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private ServerConfiguration config;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = standaloneSetup(controller).build();
        reset(messagingService);
    }

    @After
    public void tearDown() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("status", "INACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        controller.sessionOperation(request);
    }

    @Test
    public void contextLoads() {
        assertNotNull(messagingService);
        assertNotNull(controller);

    }

    @Test
    public void testCreateSession() throws Exception {
        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());
    }

    @Test
    public void testSessionRequestsWithoutStatus() throws Exception {
        JSONObject request = new JSONObject();
        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateSessionWhenAlreadyActive() throws Exception {
        // create a session first
        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());

        // again try and create the session

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateSessionWithInvalidSessionPass() throws Exception {
        // create a session first
        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", "invalid pass");

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateSessionWithoutSessionName() throws Exception {
        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetSession() throws Exception {
        // create the session

        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());

        // get the session detail first and then ask for session detail to the
        // rest

        long sessionId = controller.getSession()
                .getId();

        mockMvc.perform(get("/sessions/" + sessionId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetSessionWithIncorrectId() throws Exception {
        // create the session

        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());
        // ask for session with the an invalid session id

        mockMvc.perform(get("/sessions/123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetSessionWithNoOpenSession() throws Exception {

        // ask for session when the session is not open
        mockMvc.perform(get("/sessions/123").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testAddUserToSession() throws Exception {
        // create the session first

        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());

        // add the user to the session
        JSONObject userRq = new JSONObject();
        userRq.put("name", "test user");
        userRq.put("pushToken", "push token");
        JSONObject pref = new JSONObject();
        pref.put("distance", 10.5);
        userRq.put("pref", pref);

        mockMvc.perform(post("/sessions/users").contentType(MediaType.APPLICATION_JSON)
                .content(userRq.toString()))
                .andExpect(status().isOk());

        // assert that the user was added successfully
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());
    }
    
    @Test
    public void testAddUserToSessionWithIntDistance() throws Exception {
        // create the session first

        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());

        // add the user to the session
        JSONObject userRq = new JSONObject();
        userRq.put("name", "test user");
        userRq.put("pushToken", "push token");
        JSONObject pref = new JSONObject();
        pref.put("distance", 10);
        userRq.put("pref", pref);

        mockMvc.perform(post("/sessions/users").contentType(MediaType.APPLICATION_JSON)
                .content(userRq.toString()))
                .andExpect(status().isOk());

        // assert that the user was added successfully
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());
    }

    @Test
    public void testAddUserWhenSessionClosed() throws Exception {
        // add the user to the session
        JSONObject userRq = new JSONObject();
        userRq.put("name", "test user");
        userRq.put("pushToken", "push token");
        JSONObject pref = new JSONObject();
        pref.put("distance", 10.5);
        userRq.put("pref", pref);

        mockMvc.perform(post("/sessions/users").contentType(MediaType.APPLICATION_JSON)
                .content(userRq.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddUserWhenAlreadyAdded() throws Exception {
        // create the session first

        JSONObject request = new JSONObject();
        request.put("status", "ACTIVE");
        request.put("name", "test session");
        request.put("passkey", config.getSessionPass());

        mockMvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isCreated());

        // add the user to the session
        JSONObject userRq = new JSONObject();
        userRq.put("name", "test user");
        userRq.put("pushToken", "push token");
        JSONObject pref = new JSONObject();
        pref.put("distance", 10.5);
        userRq.put("pref", pref);

        mockMvc.perform(post("/sessions/users").contentType(MediaType.APPLICATION_JSON)
                .content(userRq.toString()))
                .andExpect(status().isOk());

        // try adding the same user again
        mockMvc.perform(post("/sessions/users").contentType(MediaType.APPLICATION_JSON)
                .content(userRq.toString()))
                .andExpect(status().isBadRequest());

        // assert that the user was added successfully
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());
    }

    @Test
    public void testRemoveUser() throws Exception {
        // add a user first
        testAddUserToSession();

        // assert that the user is there
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());

        // get the user name
        Map<String, User> users = controller.getSession()
                .getActiveUsers();
        String[] names = users.keySet()
                .toArray(new String[users.size()]);

        assertEquals(1, names.length);
        String username = URLEncoder.encode(names[0], "utf-8");

        // remove the user now
        mockMvc.perform(delete("/sessions/users/" + username))
                .andExpect(status().isOk());

        assertEquals(0, controller.getSession()
                .getActiveUsers()
                .size());
    }

    @Test
    public void testUpdatePrefs() throws Exception {
        // add a user first
        testAddUserToSession();

        // assert that the user is there
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());

        // update the pref for the user
        JSONObject request = new JSONObject();
        JSONObject pref = new JSONObject();
        pref.put("distance", 10.5);

        request.put("pref", pref);

        // get the user name
        Map<String, User> users = controller.getSession()
                .getActiveUsers();
        String[] names = users.keySet()
                .toArray(new String[users.size()]);

        assertEquals(1, names.length);
        String username = URLEncoder.encode(names[0], "utf-8");

        mockMvc.perform(put("/sessions/users/" + username).contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAllCustomers() throws Exception {
        // add a user first
        testAddUserToSession();

        // assert that the user is there
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());

        Map<String, User> users = controller.getSession()
                .getActiveUsers();
        String[] names = users.keySet()
                .toArray(new String[users.size()]);
        assertEquals(1, names.length);

        // get the list of users
        mockMvc.perform(get("/sessions/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testNotifyUser() throws Exception {
        // create the session and the user
        testAddUserToSession();

        // assert that the user is there
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());

        Map<String, User> users = controller.getSession()
                .getActiveUsers();
        String[] names = users.keySet()
                .toArray(new String[users.size()]);
        assertEquals(1, names.length);
        String username = URLEncoder.encode(names[0], "utf-8");

        // make the request
        JSONObject req = new JSONObject();
        req.put("passkey", config.getSessionPass());
        JSONObject message = new JSONObject();
        message.put("test", "test");
        req.put("message", message);

        mockMvc.perform(post(("/sessions/users/" + username + "/notify")).contentType(MediaType.APPLICATION_JSON)
                .content(req.toString()))
                .andExpect(status().isOk());

        // assert that send message was called
        verify(messagingService, times(1)).sendMessage(any());
    }

    @Test
    public void testNotifyUserWithoutSession() throws Exception {
        // make the request
        JSONObject req = new JSONObject();
        req.put("passkey", config.getSessionPass());
        JSONObject message = new JSONObject();
        message.put("test", "test");
        req.put("message", message);

        mockMvc.perform(post(("/sessions/users/123/notify")).contentType(MediaType.APPLICATION_JSON)
                .content(req.toString()))
                .andExpect(status().isUnauthorized());

        verify(messagingService, times(0)).sendMessage(any());
    }

    @Test
    public void testNotifyUserWithPasskeyMismatch() throws Exception {
        // create the session and the user
        testAddUserToSession();

        // assert that the user is there
        assertEquals(1, controller.getSession()
                .getActiveUsers()
                .size());

        Map<String, User> users = controller.getSession()
                .getActiveUsers();
        String[] names = users.keySet()
                .toArray(new String[users.size()]);
        assertEquals(1, names.length);
        String username = URLEncoder.encode(names[0], "utf-8");

        // make the request
        JSONObject req = new JSONObject();
        req.put("passkey", "wrong key");
        JSONObject message = new JSONObject();
        message.put("test", "test");
        req.put("message", message);

        mockMvc.perform(post(("/sessions/users/" + username + "/notify")).contentType(MediaType.APPLICATION_JSON)
                .content(req.toString()))
                .andExpect(status().isUnauthorized());

        // assert that send message was called
        verify(messagingService, times(0)).sendMessage(any());
    }

}
