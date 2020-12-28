package com.example.phonedialer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    EditText phoneNumberField;
    Intent myService;

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_id), name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void doDial() {
        if (phoneNumberField.getText().length() <= 3) {
            Toast.makeText(this, "Please Enter the Valid Number", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL);

            String hash = phoneNumberField.getText().toString();
            if (hash.contains("#")) {
                hash.replace("#", "%23");
            }

            intent.setData(Uri.parse("tel:" + hash));

            // Check if CALL_PHONE permission is unavailable.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // Ask for permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                // return;
            } else {
                // Log.d("main", "Calling Out Now");
                startActivity(intent);
            }
        }
    }

    public void onNumberButtonClick(View view) {
        switch (view.getId()) {
            case R.id.num0:
                phoneNumberField.setText(phoneNumberField.getText() + "0");
                break;
            case R.id.num1:
                phoneNumberField.setText(phoneNumberField.getText() + "1");
                break;
            case R.id.num2:
                phoneNumberField.setText(phoneNumberField.getText() + "2");
                break;
            case R.id.num3:
                phoneNumberField.setText(phoneNumberField.getText() + "3");
                break;
            case R.id.num4:
                phoneNumberField.setText(phoneNumberField.getText() + "4");
                break;
            case R.id.num5:
                phoneNumberField.setText(phoneNumberField.getText() + "5");
                break;
            case R.id.num6:
                phoneNumberField.setText(phoneNumberField.getText() + "6");
                break;
            case R.id.num7:
                phoneNumberField.setText(phoneNumberField.getText() + "7");
                break;
            case R.id.num8:
                phoneNumberField.setText(phoneNumberField.getText() + "8");
                break;
            case R.id.num9:
                phoneNumberField.setText(phoneNumberField.getText() + "9");
                break;
            case R.id.charSharp:
                phoneNumberField.setText(phoneNumberField.getText() + "#");
                break;
            case R.id.charStar:
                phoneNumberField.setText(phoneNumberField.getText() + "*");
                break;
            case R.id.buttonDelete:
                phoneNumberField.setText("");
                break;
            case R.id.buttonDial:
                doDial();
                break;
            default:
                break;
        }
    }

    public boolean isConnectedToThisServer(String host) throws IOException, InterruptedException {
        final String command = "ping -c 1 google.com";
        return Runtime.getRuntime().exec(command).waitFor() == 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        myService = new Intent(this, MyService.class);

        phoneNumberField = findViewById(R.id.phoneNumberField);

        // startService(new Intent(this, MyService.class));
        startService(myService);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }

        // Ability to switch WiFi
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        SwitchCompat wifiSwitch = findViewById(R.id.wifi_switch);
        wifiSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> wifiManager.setWifiEnabled(isChecked)));

        Log.d("main", "Activity Created");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("main", "Activity Started");
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update UI
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // -Interface
        TextView interface_status = findViewById(R.id.internet_status);
        String infs = Arrays.stream(manager.getAllNetworks())
                .filter(n -> manager.getNetworkCapabilities(n).hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .findFirst()
                .map(n -> manager.getLinkProperties(n))
                .map(n -> n.getInterfaceName())
                .orElse("None");

        interface_status.setText(infs);

        // Check Connectivity at Start (a.k.a. onCreate)

        // -Wifi
        Switch wifiSwitch = findViewById(R.id.wifi_switch);
        boolean isWiFiOn = Arrays.stream(manager.getAllNetworks())
                .map(network -> manager.getNetworkInfo(network))
                .anyMatch(networkInfo -> networkInfo.getType() == ConnectivityManager.TYPE_WIFI);

        wifiSwitch.setChecked(isWiFiOn);

        // -Mobile Data
        TextView mobileData_status = findViewById(R.id.mobileData_status);
        boolean isMobile = Arrays.stream(manager.getAllNetworks())
                .map(network -> manager.getNetworkInfo(network))
                .anyMatch(networkInfo -> networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);

        mobileData_status.setText(isMobile ? "On" : "Off");

        // -Internet
        TextView internet_status = findViewById(R.id.internet_status);

        internet_status.setText("Off");
        try {
            boolean isConnected = isConnectedToThisServer("https://www.google.com/");
            if (isConnected) interface_status.setText("On");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("main", "Activity Resumed");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("main", "Activity Stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(myService);
        Log.d("main", "Activity Destroyed");
    }
}