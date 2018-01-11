package com.mchecca.mcp;

import android.app.Activity;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import static com.mchecca.mcp.Settings.LOG_TAG;

public class MqttUtil {
    static MqttUtil instance;
    IMqttAsyncClient mqttClient;
    Activity activity;
    SmsUtil smsUtil;
    String sendTopic;
    String eventTopic;
    String smsReceivedTopic;
    String pingTopic;
    SmsListener smsListener;

    public MqttUtil(Activity activity, String mqttUrl, String clientId) {
        smsUtil = new SmsUtil(activity);
        sendTopic = clientId + "/sms/send";
        eventTopic = clientId + "/sms/event";
        smsReceivedTopic = clientId + "/sms/receive";
        pingTopic = clientId + "/ping";
        this.mqttClient = new MqttAndroidClient(activity.getApplicationContext(), mqttUrl, MqttClient.generateClientId());
        this.mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i(LOG_TAG, "Connected to " + serverURI);
                // TODO: Subscribe to topics
                try {
                    mqttClient.subscribe(new String[] {sendTopic, pingTopic}, new int[] {0, 0});
                    Log.i(LOG_TAG, "Subscribed to: " + sendTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Unable to subscribe to topic");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.i(LOG_TAG, "Lost connection: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(LOG_TAG, "New Message; Topic: " + topic + ", Message: " + message.toString());
                if (topic.equals(sendTopic)) {
                    JSONObject smsMessage = new JSONObject(new String(message.getPayload()));
                    handleSmsMessage(smsMessage);
                } else if (topic.equals(pingTopic)) {
                    handlePingMessage();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(LOG_TAG, "Delivered message id: " + token.getMessageId());
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(LOG_TAG, "Successful connection");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(LOG_TAG, "Failed connection: " + exception.getMessage());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
            Log.e(LOG_TAG, "Error connecting to MQTT server");
        }
        instance = this;
    }

    public static MqttUtil getInstance() {
        return instance;
    }

    public boolean sendNewSmsMessage(String number, String message) {
        JSONObject smsMessage = new JSONObject();
        try {
            smsMessage.put("number", number);
            smsMessage.put("message", message);
            smsMessage.put("date", System.currentTimeMillis() / 1000);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Unable to create SMS JSON");
            return false;
        }
        return sendMqttMessage(smsReceivedTopic, smsMessage.toString());
    }

    boolean sendMqttMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Error sending MQTT messsage");
        }
        return false;
    }

    void handleSmsMessage(JSONObject sendMsg) {
        try {
            String number = sendMsg.getString("number");
            String smsMessage = sendMsg.getString("message");
            Log.d(LOG_TAG, "[SMS send] Number: " + number + ", Message: " + smsMessage);
            if (smsUtil.sendSms(number, smsMessage)) {
                JSONObject successMsg = new JSONObject();
                successMsg.put("type", "sms_sent");
                successMsg.put("number", number);
                successMsg.put("message", smsMessage);
                successMsg.put("success", true);
                successMsg.put("date", System.currentTimeMillis() / 1000);
                sendMqttMessage(eventTopic, successMsg.toString());
            } else {
                throw new Exception("Unable to send SMS message");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Unable to parse JSON");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    void handlePingMessage() {
        JSONObject pingResponse = new JSONObject();
        try {
            pingResponse.put("type", "ping");
            pingResponse.put("date", System.currentTimeMillis() / 1000);
            sendMqttMessage(eventTopic, pingResponse.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Unable to create ping message");
        }
    }
}
