package com.mchecca.mcp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import static com.mchecca.mcp.Settings.LOG_TAG;

public class SmsUtil {
    Activity activity;

    public SmsUtil(Activity activity) {
        this.activity = activity;
    }

    public boolean sendSms(String number, String message) {
        try {
            String SENT = LOG_TAG + "_SMS_SENT";
            Intent myIntent = new Intent(SENT);
            PendingIntent sentPI = PendingIntent.getBroadcast(activity.getApplicationContext(), 0, myIntent, 0);

            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> messageParts = sms.divideMessage(message);
            ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
            int messageCount = messageParts.size();

            for (int i = 0; i < messageCount; i++) {
                sentPendingIntents.add(sentPI);
            }

            sms.sendMultipartTextMessage(number, null, messageParts, sentPendingIntents, null);
            addMessageToSent(number, message);
            Log.d(LOG_TAG, "Send message \"" + message +  "\"to " + number);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "undefined Error: SMS sending failed");
            return false;
        }
        return true;
    }

    private void addMessageToSent(String phoneNumber, String message) {
        ContentValues sentSms = new ContentValues();
        sentSms.put("address", phoneNumber);
        sentSms.put("body", message);

        ContentResolver contentResolver = activity.getContentResolver();
        contentResolver.insert(Uri.parse("content://sms/sent"), sentSms);
    }
}
