package com.dev.karandeepsingh.gcalsender;

/**
 * Created by Karan Deep Singh on 1/23/2016.
 */
public class EventMetaData {
    String eventName;
    String eventDate;
    Boolean isChecked;

    EventMetaData(String eventName,String eventDate) {
        this.eventName = eventName;
        this.eventDate=eventDate;
        this.isChecked = false;
    }
}
