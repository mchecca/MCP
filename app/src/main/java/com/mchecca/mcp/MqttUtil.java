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

import static com.mchecca.mcp.Settings.LOG_TAG;

public class MqttUtil {
    IMqttAsyncClient mqttClient;

    public MqttUtil(Activity activity, String mqttUrl) {
        this.mqttClient = new MqttAndroidClient(activity.getApplicationContext(), mqttUrl, MqttClient.generateClientId());
        this.mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.i(LOG_TAG, "Connected to " + serverURI);
                // TODO: Subscribe to topics
                try {
                    mqttClient.subscribe("#", 0);
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
    }

    public boolean sendMqttMessage(String topic, String message) {
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
}
