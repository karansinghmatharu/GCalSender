package com.dev.karandeepsingh.gcalsender;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Karan Deep Singh on 1/23/2016.
 */
public class MainActivity extends Activity {
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    com.google.api.services.calendar.Calendar mService;
    GoogleAccountCredential credential;
    private Map<Integer, EventMetaData> selectedEvents;
    private String emailBodyTextArr[];
    private StringBuilder builder = new StringBuilder();
    private RecyclerView rv;
    private Button clickButton;
    private EditText editText;
    private List<EventMetaData> eventMetaDataList;
    private RecycleVAdapter adapter;
    private ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(false);
        progressBar.setMessage("Fetching Google Calendar Data....");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        rv = (RecyclerView) findViewById(R.id.rv);
        clickButton = (Button) findViewById(R.id.sendButton);
        editText = (EditText) findViewById(R.id.editText);
        clickButton.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.INVISIBLE);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Calendar API Android Quickstart")
                .build();

        eventMetaDataList = new ArrayList<>();
        adapter = new RecycleVAdapter(eventMetaDataList);
        rv.setAdapter(adapter);
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedEvents = adapter.getSelectedEvents();
                emailBodyTextArr = new String[selectedEvents.size()];
                if (selectedEvents.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Kindly select some events from the check box", Toast.LENGTH_LONG).show();
                } else {

                    String[] arr = new String[1];
                    String emails = editText.getText().toString();
                    if (emails.matches("")) {
                        Toast.makeText(getApplicationContext(), "Enter some email id/s", Toast.LENGTH_LONG).show();
                    } else {
                        for (Map.Entry<Integer, EventMetaData> entry : selectedEvents.entrySet()) {
                            emailBodyTextArr = Utils.processDate(entry.getValue().eventInfo);
                            builder.append(emailBodyTextArr[0] + " on ");
                            builder.append(emailBodyTextArr[1] + '\n');
                        }
                        arr[0] = emails;
                        shareToGMail(arr, "Notification from GCalSender selected Events", builder.toString());
                        onBackClick();
                    }
                }
            }
        });
    }

    public void onBackClick() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.setLength(0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            Toast.makeText(getApplicationContext(), "Google Play Services required:after installing, close and relaunch this app.", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == RESULT_OK) {
                    refreshResults();
                } else {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        refreshResults();
                        progressBar.show();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Account unspecified", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    refreshResults();
                } else {
                    chooseAccount();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void refreshResults() {
        if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                if (eventMetaDataList.size() == 0)
                    progressBar.show();
                new GoogleApiAsyncTask(this).execute();


            } else {
                Toast.makeText(getApplicationContext(), "No network connection available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateData(final List<String> dataStrings) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dataStrings == null) {
                    Toast.makeText(getApplicationContext(), "Error retrieving data! Retry after Sometime", Toast.LENGTH_LONG).show();
                    progressBar.dismiss();

                } else if (dataStrings.size() == 0) {
                    Toast.makeText(getApplicationContext(), "No data found on Calendar", Toast.LENGTH_LONG).show();
                    progressBar.dismiss();

                } else {
                    eventMetaDataList.clear();
                    for (int count = 0; count < dataStrings.size(); count++) {
                        EventMetaData metadata = new EventMetaData(dataStrings.get(count));
                        eventMetaDataList.add(metadata);
                    }
                    adapter.setMetadataArray(eventMetaDataList);
                    clickButton.setVisibility(View.VISIBLE);
                    editText.setVisibility(View.VISIBLE);
                    progressBar.dismiss();
                }
            }
        });
    }

    public void updateStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void chooseAccount() {
        startActivityForResult(
                credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        MainActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    private void shareToGMail(String[] email, String subject, String content) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Events Scheduled are as follows :- " + '\n' + content);
        final PackageManager pManager = getApplicationContext().getPackageManager();
        final List<ResolveInfo> matches = pManager.queryIntentActivities(emailIntent, 0);
        ResolveInfo best = null;
        for (final ResolveInfo info : matches)
            if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.toLowerCase().contains("gmail"))
                best = info;
        if (best != null)
            emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getApplicationContext().startActivity(emailIntent);
    }
}