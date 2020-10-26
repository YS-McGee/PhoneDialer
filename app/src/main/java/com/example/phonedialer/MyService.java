package com.example.phonedialer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MyService extends Service {

    private TelephonyManager telephonyManager;
    private PhoneStateListener listener;
    private boolean isOnCall;

    PacketSniffer sniffer = new PacketSniffer();
    Thread innerThread = new Thread(sniffer);

    private BroadcastReceiver bcReceiver;

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
                sniffer.stop();
            }
            else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                Log.d("callstate", "Incoming call..");
                Toast.makeText(context, "Incoming Call...", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCreate() {

        this.bcReceiver = new CallReceiver();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        this.registerReceiver(this.bcReceiver, intentFilter);

        Log.d("service", "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "Service Started");

        // notification action for opening Wifi-setting
        Intent wifiIntent = new Intent(this, MainActivity.class);
        PendingIntent wifiPendingIntent = PendingIntent.getActivities(this, 0, new Intent[]{wifiIntent}, 0);

        Intent broadcastIntent = new Intent(this, NotifReceiver.class);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ALERT!!")
                .setContentText("Your Network Traffic is under attack!!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(wifiPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher_foreground, getString(R.string.setting), actionIntent);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define

        Runnable notif = new Runnable() {
            public void run() {
                Log.d("process", "notif thread run");
                notificationManager.notify(1, builder.build());
            }
        };

        // PacketSniffer sniffer = new PacketSniffer();
        // Thread innerThread = new Thread(sniffer);
        innerThread.setPriority(Thread.MAX_PRIORITY);
        innerThread.start();

        Thread n = new Thread(notif);
        n.setPriority(Thread.MAX_PRIORITY);
        n.start();

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
        this.unregisterReceiver(this.bcReceiver);

        Log.d("process", "Service Destroyed");
    }
}