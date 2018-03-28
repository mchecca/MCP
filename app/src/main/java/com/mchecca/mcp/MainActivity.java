package com.mchecca.mcp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;

import static com.mchecca.mcp.Settings.LOG_TAG;

public class MainActivity extends AppCompatActivity {
    static String MQTT_URL_KEY = "mqttUrl";
    static String CLIENT_ID_KEY = "clientId";

    MqttUtil mqttUtil;
    Button connectButton;
    EditText clientIdEditText;
    EditText mqttServerEditText;
    EditText logEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = findViewById(R.id.connectButton);
        clientIdEditText = findViewById(R.id.clientIdEditText);
        mqttServerEditText = findViewById(R.id.mqttServerEditText);
        logEditText = findViewById(R.id.logEditText);

        // Load settings
        final SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        String mqttUrl = sharedPrefs.getString(MQTT_URL_KEY, "");
        String clientId = sharedPrefs.getString(CLIENT_ID_KEY, "");
        if (mqttUrl.length() > 0) {
            mqttServerEditText.setText(mqttUrl);
        }
        if (clientId.length() > 0) {
            clientIdEditText.setText(clientId);
        }

        final MainActivity mainActivity = this;
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mqttUrl = mqttServerEditText.getText().toString();
                String clientId = clientIdEditText.getText().toString();
                mqttUtil = new MqttUtil(mainActivity, mqttUrl, clientId);
                // Save settings
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(MQTT_URL_KEY, mqttUrl);
                editor.putString(CLIENT_ID_KEY, clientId);
                editor.commit();
            }
        });
    }

    protected void logMessage(String message) {
        String dateString = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()).toString();
        String logMsg = dateString + ": " + message + "\n";
        logEditText.append(logMsg);
    }

    public void logDebug(String message) {
        logMessage(message);
        Log.d(LOG_TAG, message);
    }

    public void logInfo(String message) {
        logMessage(message);
        Log.i(LOG_TAG, message);
    }

    public void logError(String message) {
        logMessage(message);
        Log.e(LOG_TAG, message);
    }
}
