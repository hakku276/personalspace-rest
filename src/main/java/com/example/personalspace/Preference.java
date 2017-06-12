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

        distance = (double) map.get("distance");
    }
}