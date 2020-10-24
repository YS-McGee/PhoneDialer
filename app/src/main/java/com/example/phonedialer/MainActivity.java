package com.example.phonedialer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button btnOne;
    Button btnTwo;
    Button btnThree;
    Button btnFour;
    Button btnFive;
    Button btnSix;
    Button btnSeven;
    Button btnEight;
    Button btnNine;
    Button btnStar;
    Button btnZero;
    Button btnHash;
    Button btnDelete;
    Button btnDial;

    EditText input;

    private Switch wifiSwitch;
    private WifiManager wifiManger;
    private TextView mobileDataTextView;

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void one(View v) { onButtonClick(btnOne, input, "1"); }
    public void two(View v) { onButtonClick(btnTwo, input, "2"); }
    public void three(View v) { onButtonClick(btnThree, input, "3"); }
    public void four(View v) { onButtonClick(btnFour, input, "4"); }
    public void five(View v) { onButtonClick(btnFive, input, "5"); }
    public void six(View v) { onButtonClick(btnSix, input, "6"); }
    public void seven(View v) { onButtonClick(btnSeven, input, "7"); }
    public void eight(View v) { onButtonClick(btnEight, input, "8"); }
    public void nine(View v) { onButtonClick(btnNine, input, "9"); }
    public void star(View v) { onButtonClick(btnStar, input, "*"); }
    public void zero(View v) { onButtonClick(btnZero, input, "0"); }
    public void hash(View v) { onButtonClick(btnHash, input, "#"); }
    public void onDelete(View v) { input.setText(""); }
    public void onDial(View v) {
        if (input.getText().length() <= 3) {
            Toast.makeText(this, "Please Enter the Valid Number", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent  = new Intent(Intent.ACTION_CALL);

            String hash = input.getText().toString();
            if (hash.contains("#")) {
                hash.replace("#", "%23");
            }

            intent.setData(Uri.parse("tel:" + hash));

            // Check if CALL_PHONE permission is unavailable.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // Ask for permission.
                ActivityCompat.requestPermissions(MainActivity.this, new  String[] {Manifest.permission.CALL_PHONE}, 1);
                // return;
            }
            else {
                // Log.d("main", "Calling Out Now");
                startService(new Intent(this, MyService.class));
                startActivity(intent);
            }
        }
    }

    public void onButtonClick(Button button, EditText inputNumber, String number) {
        String cache = input.getText().toString();
        inputNumber.setText(cache + number);
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

        btnOne = findViewById(R.id.buttonOne);
        btnTwo = findViewById(R.id.buttonTwo);
        btnThree = findViewById(R.id.buttonThree);
        btnFour = findViewById(R.id.buttonFour);
        btnFive = findViewById(R.id.buttonFive);
        btnSix = findViewById(R.id.buttonSix);
        btnSeven = findViewById(R.id.buttonSeven);
        btnEight = findViewById(R.id.buttonEight);
        btnNine = findViewById(R.id.buttonNine);
        btnStar = findViewById(R.id.buttonStar);
        btnZero = findViewById(R.id.buttonZero);
        btnHash = findViewById(R.id.buttonHash);
        btnDelete = findViewById(R.id.buttonDelete);
        btnDial = findViewById(R.id.buttonDial);
        input = findViewById(R.id.editText);

        Log.d("main", "Activity Created");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("main", "Activity onStart");

        wifiSwitch = findViewById(R.id.wifi_switch);
        wifiManger = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        TextView internetTextView = findViewById(R.id.internet_textView);
        mobileDataTextView = findViewById(R.id.mobileTextview);

        // Check Connectivity at Start (a.k.a. onCreate)
        Boolean isConnected = false, isWiFi = false, isMobile = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null) {
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
            isMobile = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
            isConnected = activeNetwork.isConnectedOrConnecting();
        }
        try {
            if(isConnectedToThisServer("https://www.google.com/")) {
                            Log.d("main", "Yes, Connected to Google");
                            internetTextView.setText("Internet               On");
            } else {
                internetTextView.setText("Internet              Off");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (isMobile) {
            Log.d("main", "Yes, Mobile");
            mobileDataTextView.setText("Mobile Data       On");
        } else mobileDataTextView.setText("Mobile Data       Off");
//        if (isConnected) {
//            if (isWiFi) { Log.d("main", "Yes, WiFi");
//                try {
//                    if(isConnectedToThisServer("https://www.google.com/")) {
//                        Log.d("main", "Yes, Connected to Google");
//                        internetTextView.setText("Internet               On");
//                    } else { Log.d("main", "No Google Connection"); }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            }
//            if (isMobile) {
//                Log.d("main", "Yes, Mobile");
//                mobileDataTextView.setText("Mobile Data       On");
//                try {
//                    if (isConnectedToThisServer("https://www.google.com/")) {
//                        Log.d("main", "Yes, Connected to Google");
//                        internetTextView.setText("Internet               On");
//                    } else {
//                        Log.d("main", "No Google Connection");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        } else { Log.d("main", "Not Connected on any"); }

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    wifiManger.setWifiEnabled(true);
                    wifiSwitch.setText("Wi-Fi                    On");
                } else {
                    wifiManger.setWifiEnabled(false);
                    wifiSwitch.setText("Wi-Fi                   Off");
                }
            }
        });

        if (wifiManger.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
            wifiSwitch.setText("Wi-Fi                    On");
        } else {
            wifiSwitch.setChecked(false);
            wifiSwitch.setText("Wi-Fi                   Off");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Toast.makeText(this, "Activity Resumed", Toast.LENGTH_SHORT);
        Log.d("main", "Activity Resumed");
    }

    @Override
    public void onStop() {
        // stopService(new Intent(this, MyService.class));
        super.onStop();
        // Toast.makeText(this, "Activity Stopped", Toast.LENGTH_SHORT);
        Log.d("main", "Activity Stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent service = new Intent(this, MyService.class);
        stopService(service);
        Log.d("main", "Activity Destroyed");
    }
}