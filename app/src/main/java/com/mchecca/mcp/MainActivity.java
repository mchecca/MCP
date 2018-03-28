package com.mchecca.mcp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    static String[] PERMISSIONS = new String[] {
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

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

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 0);
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (checkPermissions()) {
            logInfo("All permissions granted");
        }
    }

    protected boolean checkPermissions() {
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                connectButton.setEnabled(false);
                logError("Missing permission: " + p);
                return false;
            }
        }
        connectButton.setEnabled(true);
        return true;
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
