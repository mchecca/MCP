package com.mchecca.mcp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;

import static com.mchecca.mcp.Settings.LOG_TAG;

public class MainActivity extends AppCompatActivity {
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

        final MainActivity mainActivity = this;
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mqttUrl = mqttServerEditText.getText().toString();
                String clientId = clientIdEditText.getText().toString();
                mqttUtil = new MqttUtil(mainActivity, mqttUrl, clientId);
            }
        });
    }

    protected void logMessage(String message) {
        String logMsg = new Date().toString() + ": " + message + "\n";
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
