package com.example.personalspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false, of = "name")
public class User {

    /**
     * The name of the user
     */
    private String name;

    /**
     * The push token related to the device
     */
    @JsonIgnore
    private String pushToken;

    /**
     * The preference of the user
     */
    private Preference pref;

    @SuppressWarnings("unchecked")
    public void fromMap(Map<String, Object> map) {
        if (!map.containsKey("name") || !map.containsKey("pref") || !map.containsKey("pushToken")) {
            throw new IllegalArgumentException("Invalid Map Input");
        }
        name = (String) map.get("name");
        pushToken = (String) map.get("pushToken");
        pref = new Preference();
        if (map.get("pref") instanceof Map<?, ?>) {
            pref.fromMap((Map<String, Object>) map.get("pref"));
        }
    }
}
