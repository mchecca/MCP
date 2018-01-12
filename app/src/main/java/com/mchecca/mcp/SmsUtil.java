package com.mchecca.mcp;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import java.util.ArrayList;


public class SmsUtil {
    MainActivity activity;

    public SmsUtil(MainActivity activity) {
        this.activity = activity;
    }

    public boolean sendSms(String number, String message) {
        try {
            Intent myIntent = new Intent("com.mchecca.mcp.SmsUtil");
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
            activity.logDebug("Send message \"" + message +  "\"to " + number);
        } catch (Exception e) {
            e.printStackTrace();
            activity.logError("Undefined Error: SMS sending failed");
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
