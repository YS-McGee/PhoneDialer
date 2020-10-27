package com.example.phonedialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.provider.Settings.ACTION_WIFI_SETTINGS;

public class NotifReceiver extends BroadcastReceiver {

    @Override
    public void onReceive (Context context , Intent intent) {
        // Log.d("broadcast", "1111");
        Intent Mintent = new Intent(ACTION_WIFI_SETTINGS);
        Mintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Mintent);
    }
}
