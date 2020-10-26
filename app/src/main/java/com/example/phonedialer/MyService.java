package com.example.phonedialer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MyService extends Service {

    private TelephonyManager telephonyManager;
    private PhoneStateListener listener;
    private boolean isOnCall;

    private PacketSniffer sniffer;

    @Override
    public void onCreate() {
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
                Log.d("service", "Room Service. Hi!!!!!");
                SystemClock.sleep(10000);
                int i = 0;
                notificationManager.notify(1, builder.build());
//                while (true) {
//                    String s = Integer.toString(i);
//                    Log.d("service", "asd");
//                    SystemClock.sleep(1000);
//                    i++;
//                }
                // notificationManager.notify(1, builder.build());
            }
        };

        Runnable callState = new Runnable() {
            public void run() {
               // Log.d("service", "callstate state");

                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

//                while (true) {
//                    SystemClock.sleep(500);
//                    // Log.d("service", TelephonyManager.EXTRA_STATE);
//                    int s = tm.getCallState();
//                    if(s == TelephonyManager.CALL_STATE_IDLE) {
//                        // Log.d("service", "call state idle...");
//                        Log.d("service", "call state idle");
//                    } else if(s == TelephonyManager.CALL_STATE_OFFHOOK) {
//                        Log.d("service", "call state off-hook...");
//                    } else if(s == TelephonyManager.CALL_STATE_RINGING) {
//                        Log.d("service", "call state ringing...");
//                    }
//                }
            }
        };

        sniffer = new PacketSniffer();
        Thread innerThread = new Thread(sniffer);
        innerThread.setPriority(Thread.MAX_PRIORITY);
        innerThread.start();

        Thread n = new Thread(notif);
        // Thread c = new Thread(callState);
        n.setPriority(Thread.MAX_PRIORITY);
        // c.setPriority(Thread.NORM_PRIORITY);
        n.start();
        // c.start();

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
        Log.d("service", "Service Destroyed");
    }
}