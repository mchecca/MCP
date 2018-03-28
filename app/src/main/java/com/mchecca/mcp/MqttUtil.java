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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MqttUtil {
    static MqttUtil instance;
    IMqttAsyncClient mqttClient;
    MainActivity activity;
    SmsUtil smsUtil;
    String sendTopic;
    String eventTopic;
    String rpcRequestTopic;
    String rpcReplyTopic;
    String smsReceivedTopic;
    String connectedTopic;

    public MqttUtil(final MainActivity activity, String mqttUrl, String clientId) {
        String mqttIpPort = "tcp://";
        String username = "";
        String password = "";
        // Check to see if username and password was supplied via the URL
        if (mqttUrl.contains("@")) {
            String[] split = mqttUrl.split("@");
            String[] usernamePasswordSplit = split[0].split(":");
            username = usernamePasswordSplit[0];
            password = usernamePasswordSplit[1];
            mqttIpPort += split[1];
        } else {
            mqttIpPort += mqttUrl;
        }

        smsUtil = new SmsUtil(activity);
        sendTopic = clientId + "/sms/send";
        eventTopic = clientId + "/sms/event";
        rpcRequestTopic = clientId + "/rpc/request";
        rpcReplyTopic = clientId + "/rpc/reply";
        smsReceivedTopic = clientId + "/sms/receive";
        connectedTopic = clientId + "/connected";
        this.activity = activity;
        this.mqttClient = new MqttAndroidClient(activity.getApplicationContext(), mqttIpPort, MqttClient.generateClientId());
        this.mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                activity.logInfo("Connected to " + serverURI);
                sendMqttMessage(connectedTopic, "true", true);
                try {
                    mqttClient.subscribe(new String[] {sendTopic, rpcRequestTopic}, new int[] {2, 2});
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
                } else if (topic.equals(rpcRequestTopic)) {
                    JSONObject commandMsg = new JSONObject(new String(message.getPayload()));
                    handleRpcRequestMessage(commandMsg);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                activity.logDebug("Delivered message id: " + token.getMessageId());
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setWill(connectedTopic, "false".getBytes(), 2, true);
        if (username.length() > 0 && password.length() > 0) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
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
        return sendMqttMessage(smsReceivedTopic, smsMessage);
    }

    boolean sendMqttMessage(String topic, JSONObject jsonMessage) {
        return sendMqttMessage(topic, jsonMessage.toString());
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

    boolean sendMqttErrorMessage(String topic, String errorMessage) {
        try {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("type", "error");
            errorMsg.put("message", errorMessage);
            return sendMqttMessage(topic, errorMsg);
        } catch (JSONException e) {
            e.printStackTrace();
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
                sendMqttMessage(eventTopic, successMsg);
            } else {
                sendMqttErrorMessage(eventTopic, "Unable to send SMS message");
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

    void handleRpcRequestMessage(JSONObject requestMsg) {
        try {
            if (!requestMsg.has("id")) {
                sendMqttErrorMessage(rpcReplyTopic, "RPC with empty id");
                throw new Exception("RPC with empty id");
            }
            String id = requestMsg.getString("id");
            String command = requestMsg.getString("command");
            if (command.equals("list sms")) {
                JSONArray smsList = new JSONArray();
                for (Sms s : smsUtil.getOrderedSMS(10)) {
                    smsList.put(s.toJson());
                }
                JSONObject smsReply = new JSONObject();
                smsReply.put("id", id);
                smsReply.put("sms", smsList);
                sendMqttMessage(rpcReplyTopic, smsReply);
            } else {
                String commandErrMsg = "Invalid command: " + command;
                sendMqttErrorMessage(rpcReplyTopic, commandErrMsg);
                throw new Exception(commandErrMsg);
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
