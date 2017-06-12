package com.example.personalspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import lombok.Data;
import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a Single HCI research session
 * 
 * @author aanal
 *
 */
@Data
public class Session {

    public enum Status {
        ACTIVE("ACTIVE"), INACTIVE("INACTIVE");

        @Getter
        private String value;

        private Status(String value) {
            this.value = value;
        }
    }

    /**
     * The id of the session
     */
    private long id;

    /**
     * The name of the session
     */
    private String name;

    /**
     * The date when this session started
     */
    private Date startDate;

    /**
     * The date when this session concluded
     */
    private Date endDate;

    /**
     * The status of the session, active or inactive
     */
    private Status status;
    
    @JsonIgnore
    private Map<String, User> activeUsers;

    public Session() {
        Random rnd = new Random(new Date().getTime());
        id = rnd.nextLong();
        startDate = new Date();
        status = Status.ACTIVE;
        activeUsers = new HashMap<>();
    }
    
    public boolean addUser(User user){
        if(activeUsers.containsKey(user.getName())){
            return false;
        }
        activeUsers.put(user.getName(), user);
        return true;
    }
    
    public void removeUser(String userName){
        activeUsers.remove(userName);
    }
    
    public boolean updatePreference(String userName, Preference pref){
        if(activeUsers.containsKey(userName)){
            User user = activeUsers.get(userName);
            user.setPref(pref);
            return true;
        }
        return false;
    }

    /**
     * Converts the session detail into json
     * 
     * @return
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("startDate", startDate.getTime());
        if (endDate != null) {
            obj.put("endDate", endDate.getTime());
        }
        return obj;
    }
}
