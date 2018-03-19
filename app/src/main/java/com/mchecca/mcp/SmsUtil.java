package com.mchecca.mcp;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class SmsUtil {
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    private static final Uri SMS_SENTBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "sent");

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
            activity.logDebug("Send message \"" + message + "\"to " + number);
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
        contentResolver.insert(SMS_SENTBOX_CONTENT_URI, sentSms);
    }

    public List<Sms> getOrderedSMS(int maxResults) {
        List<Sms> res = new LinkedList<Sms>();
        final String[] projection = new String[]{"address", "body", "date", "type"};
        String sortOrder = "date DESC";
        if (maxResults > 0) sortOrder += " limit " + maxResults;
        Cursor c = activity.getApplicationContext().getContentResolver().query(SMS_CONTENT_URI,
                projection, null, null, sortOrder);
        if (c == null || !c.moveToFirst()) return res;
        Calendar calendar = Calendar.getInstance();
        do {
            String address = c.getString(c.getColumnIndexOrThrow("address"));
            int type = c.getInt(c.getColumnIndexOrThrow("type"));
            String body = c.getString(c.getColumnIndexOrThrow("body"));
            long date = c.getLong(c.getColumnIndexOrThrow("date"));
            calendar.setTimeInMillis(date);
            res.add(new Sms(address, body, getType(type), calendar.getTime()));
        } while (c.moveToNext());

        return res;
    }

    public static final Sms.Type getType(int type) {
        // from android.provider.Telephony.TextBasedSmsColumns
        switch (type) {
            case 0:
                return Sms.Type.ALL;
            case 1:
                return Sms.Type.INBOX;
            case 2:
                return Sms.Type.SENT;
            case 3:
                return Sms.Type.DRAFT;
            case 4:
                return Sms.Type.OUTBOX;
            case 5:
                return Sms.Type.FAILED;
            case 6:
                return Sms.Type.QUEUED;
            default:
                throw new IllegalStateException();
        }
    }
}
