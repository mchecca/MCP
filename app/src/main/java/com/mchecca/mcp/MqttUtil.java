package com.mchecca.mcp;

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

public class MqttUtil {
    static MqttUtil instance;
    IMqttAsyncClient mqttClient;
    MainActivity activity;
    SmsUtil smsUtil;
    String sendTopic;
    String eventTopic;
    String smsReceivedTopic;
    String connectedTopic;

    public MqttUtil(final MainActivity activity, String mqttUrl, String clientId) {
        if (!mqttUrl.startsWith("tcp://")) {
            mqttUrl = "tcp://" + mqttUrl;
        }
        smsUtil = new SmsUtil(activity);
        sendTopic = clientId + "/sms/send";
        eventTopic = clientId + "/sms/event";
        smsReceivedTopic = clientId + "/sms/receive";
        connectedTopic = clientId + "/connected";
        this.activity = activity;
        this.mqttClient = new MqttAndroidClient(activity.getApplicationContext(), mqttUrl, MqttClient.generateClientId());
        this.mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                activity.logInfo("Connected to " + serverURI);
                sendMqttMessage(connectedTopic, "true", true);
                try {
                    mqttClient.subscribe(sendTopic, 2);
                    activity.logMessage("Subscribed to: " + sendTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                    activity.logError("Unable to subscribe to topic");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                activity.logInfo("Lost connection: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                activity.logDebug("New Message; Topic: " + topic + ", Message: " + message.toString());
                if (topic.equals(sendTopic)) {
                    JSONObject smsMessage = new JSONObject(new String(message.getPayload()));
                    handleSmsMessage(smsMessage);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                activity.logDebug("Delivered message id: " + token.getMessageId());
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setWill(connectedTopic, "false".getBytes(), 2, true);
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    activity.logInfo("Successful connection");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    activity.logInfo("Failed connection: " + exception.getMessage());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
            activity.logError("Error connecting to MQTT server");
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
            activity.logError("Unable to create SMS JSON");
            return false;
        }
        return sendMqttMessage(smsReceivedTopic, smsMessage.toString());
    }

    boolean sendMqttMessage(String topic, String message) {
        return sendMqttMessage(topic, message, false);
    }

    boolean sendMqttMessage(String topic, String message, boolean retained) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttMessage.setRetained(retained);
            mqttMessage.setQos(2);
            mqttClient.publish(topic, mqttMessage);
        } catch (Exception e) {
            e.printStackTrace();
            activity.logError("Error sending MQTT messsage");
        }
        return false;
    }

    void handleSmsMessage(JSONObject sendMsg) {
        try {
            String number = sendMsg.getString("number");
            String smsMessage = sendMsg.getString("message");
            activity.logDebug("[SMS send] Number: " + number + ", Message: " + smsMessage);
            if (smsUtil.sendSms(number, smsMessage)) {
                JSONObject successMsg = new JSONObject();
                successMsg.put("type", "sms_sent");
                successMsg.put("number", number);
                successMsg.put("message", smsMessage);
                successMsg.put("success", true);
                successMsg.put("date", System.currentTimeMillis() / 1000);
                sendMqttMessage(eventTopic, successMsg.toString());
            } else {
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("type", "error");
                errorMsg.put("message", "Unable to send SMS message");
                sendMqttMessage(eventTopic, errorMsg.toString());
                throw new Exception("Unable to send SMS message");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            activity.logError("Unable to parse JSON");
        } catch (Exception e) {
            e.printStackTrace();
            activity.logError(e.getMessage());
        }
    }
}
