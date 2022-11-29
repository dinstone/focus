package com.dinstone.focus;

import java.util.HashMap;
import java.util.Map;

public class MapTest {

    public static void main(String[] args) {
        Map<String, String> m = new HashMap<>();
        
        String v = m.computeIfAbsent("key", k -> "value");
        System.out.println(v);

        v = m.computeIfAbsent("key", k -> "value2");
        System.out.println(v);

    }

}
