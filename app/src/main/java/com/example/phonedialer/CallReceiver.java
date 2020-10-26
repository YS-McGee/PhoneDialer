package com.example.phonedialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class CallReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            Log.d("callstate", "Call Started...");
            Toast.makeText(context, "Call Started...", Toast.LENGTH_LONG).show();
        }
        else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            Log.d("callstate", "Call ended...");
            Toast.makeText(context, "Call Ended...", Toast.LENGTH_LONG).show();
        }
        else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            Log.d("callstate", "Incoming call..");
            Toast.makeText(context, "Incoming Call...", Toast.LENGTH_LONG).show();
            // callStateTextView.setText("Call State  Incoming Call...");
        }
    }
}