package com.mchecca.mcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import static com.mchecca.mcp.Settings.LOG_TAG;

public class SmsListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                String number = smsMessage.getOriginatingAddress();
                String message = smsMessage.getMessageBody();
                MqttUtil mqtt = MqttUtil.getInstance();
                Log.d(LOG_TAG, "Received SMS from: " + number + ", " + message);
                if (mqtt != null) {
                    mqtt.sendNewSmsMessage(number, message);
                }
            }
        }
    }
}
