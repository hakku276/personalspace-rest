package com.example.personalspace;

import java.util.Map;
import lombok.Data;

@Data
public class Preference {

    double distance;

    public void fromMap(Map<String, Object> map) {
        if (!map.containsKey("distance")) {
            throw new IllegalArgumentException("The input map is invalid");
        }

        if(map.get("distance") instanceof Integer){
            distance = 1.0 * (int) map.get("distance");
        } else {
            distance = (double) map.get("distance");
        }
    }
}