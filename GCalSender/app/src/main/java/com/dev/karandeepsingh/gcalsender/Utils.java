package com.dev.karandeepsingh.gcalsender;

/**
 * Created by KaranDeepSingh on 1/24/2016.
 */
public class Utils {
    public final static String[] processDate(String str) {
        String[] parts = str.split("\\(", 2);
        String string1 = parts[0];
        String string2 = parts[1];
        String time = string2.substring(0, 10) + " at " + string2.substring(11, 16);
        parts[1] = time;
        return parts;
    }
}
