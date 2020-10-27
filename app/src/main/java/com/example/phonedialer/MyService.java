package com.example.phonedialer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Arrays;
import java.util.Optional;

public class MyService extends Service {

    private TelephonyManager telephonyManager;
    private PhoneStateListener listener;
    private boolean isOnCall;

    private final int notificationId_main = 666;
    private final int notificationId_alert = 777;

    PacketSniffer sniffer;
    Thread innerThread;

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

        ConnectivityManager manager = (ConnectivityManager)getSystemService(ConnectivityManager.class);
        Optional<Network> networkOptional = Arrays.stream(manager.getAllNetworks())
                .filter( n -> manager.getNetworkCapabilities(n).hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .findFirst();

        if (!networkOptional.isPresent()){
            Log.e("service", "Unable to find wifi interface.");
            return;
        }

        Optional<String >ip = manager.getLinkProperties(networkOptional.get()).getLinkAddresses().stream()
                .map( l -> l.getAddress())
                .filter( k -> !k.isLoopbackAddress())
                .map( i -> i.getHostAddress())
                .filter( h -> h.indexOf(":")<0)
                .findFirst();

        if (!ip.isPresent()){
            Log.e("service", "Unable to get IP address");
        }

        String ipToken = ip.get().split("\\.")[0];

        sniffer = new PacketSniffer(ipToken, this);
        innerThread = new Thread(sniffer);

        this.bcReceiver = new CallReceiver();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        this.registerReceiver(this.bcReceiver, intentFilter);

        Log.d("service", "Service Created");

        // 建立 Foreground Service
        String channelId = getString(R.string.channel_id);
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setContentTitle(getString(R.string.channel_name))
                .setContentText(getString(R.string.channel_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        startForeground(notificationId_main, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "Service Started");

        // 測試通知
        // createAlert();

        // 執行監聽執行緒
        innerThread.setPriority(Thread.MAX_PRIORITY);
        innerThread.start();

        return Service.START_NOT_STICKY;
    }

    public void createAlert() {
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

        notificationManager.notify(notificationId_alert, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(this.bcReceiver);

        Log.d("process", "Service Destroyed");
    }
}