package com.dev.karandeepsingh.gcalsender;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Karan Deep Singh on 1/23/2016.
 */
public class GoogleApiAsyncTask extends AsyncTask<Void, Void, Void> {
    private MainActivity mActivity;


    GoogleApiAsyncTask(MainActivity activity) {
        this.mActivity = activity;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {

            mActivity.updateData(getDataFromApi());

        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            mActivity.showGooglePlayServicesAvailabilityErrorDialog(
                    availabilityException.getConnectionStatusCode());

        } catch (UserRecoverableAuthIOException userRecoverableException) {
            mActivity.startActivityForResult(
                    userRecoverableException.getIntent(),
                    MainActivity.REQUEST_AUTHORIZATION);

        } catch (IOException e) {
            mActivity.updateStatus("The following error occurred: " +
                    e.getMessage());
        }
        return null;
    }

    private List<String> getDataFromApi() throws IOException {

        DateTime now = new DateTime(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 14);
        Long fourteenDays = cal.getTimeInMillis();
        DateTime nextFourteen = new DateTime(fourteenDays);
        Log.d("karan", "Today Date= " + now + "  14thDay date= " + nextFourteen);
        List<String> eventStrings = new ArrayList<String>();
        Events events = mActivity.mService.events().list("primary")
                .setMaxResults(100)
                .setTimeMin(now)
                .setTimeMax(nextFourteen)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        List<Event> items = events.getItems();

        for (Event event : items) {
            DateTime start = event.getStart().getDateTime();
            if (start == null) {

                start = event.getStart().getDate();
            }
            eventStrings.add(
                    String.format("%s (%s)", event.getSummary(), start));
        }
        return eventStrings;
    }

}