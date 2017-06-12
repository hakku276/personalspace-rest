package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.firebase.MessagingService;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { MockServletContext.class, MockServerConfiguration.class })
@WebAppConfiguration
public class PersonalSpaceApplicationTests {

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

}
