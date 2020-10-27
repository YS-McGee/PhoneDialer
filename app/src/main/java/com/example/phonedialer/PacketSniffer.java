package com.example.phonedialer;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class PacketSniffer implements Runnable {

    private boolean looping;
    private Process process;
    private final StateTracer tracer;
    private final String hostToken;

    public PacketSniffer(String hostToken) {
        looping = true;
        tracer = new StateTracer(8);

        this.hostToken = hostToken;

        Log.d(TAG, "Host Token: "+hostToken);
    }

    private final String TAG = "sniff";
    private final String[] commands = {
            "su", "-c",
            "tcpdump", "-ttt", "-q", "-n", "-l", "udp", "port", "4500", "-i", "any"
            //,"-c 10"
            // su -c tcpdump -tttqnlS udp port 4500 -i any
    };

    private double pktNumber = 0, checker;
    int flag = 0;
    Timer timer = new Timer();

    class checkPktTask extends TimerTask {
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void run() {
            // Log.d("sniff", "time's up");
            // timer.cancel(); //Terminate the timer thread
            Log.d("sniff", Double.toString(pktNumber));
            if (pktNumber == 0) {
                ++flag;
                if (flag > 50) {
                    while (true){
                        Log.d("sniff", "ATK!!!!!!!!!!");
                    }
                }
            } else {
                flag = 0;
            }
            pktNumber = 0;
        }
    }

    @Override
    public void run() {

        // 開啟 Process
        try {
            Log.d("sniff", "Listening.");
            process = new ProcessBuilder().command(commands).start();
        } catch (IOException e) {
            Log.e(TAG, "Unable to open shell");
            e.printStackTrace();
            return;
        }

        //初始化
        tracer.initial();

        // 傾聽 Stdout
        Log.d(TAG, "-- Captured Packets --");
        Scanner scanner = new Scanner(process.getInputStream());
        try {

            String timeStr;
            long time;

            String ip, src, dst, protocol;

            int len, index = 1, replace_index = 1; // pkt index
            float ftime, avg_time = 0, tmp;
            float [] farray = new float[105];

            // 輸出格式:
            //   TT IP XX.XX.XX.XX.PORT > XX.XX.XX.XX.PORT: UDP, length NN
            while (scanner.hasNext()) {
                timeStr = scanner.next();
                ip = scanner.next();
                src = scanner.next();
                scanner.next();
                dst = scanner.next();
                protocol = scanner.next();
                scanner.next();
                len = scanner.nextInt();

                // 非 IP4 封包
                if (!ip.startsWith("IP")) continue;

                // 非 UDP 封包
                if (!protocol.startsWith("UDP")) continue;

                // TODO - 根據 IP 過濾封包

                // 解析時間
                timeStr = timeStr.substring(timeStr.lastIndexOf(':')+1);
                timeStr = timeStr.replace('.', '0');
                time = Long.valueOf(timeStr);
                time = time/10000000 + time%1000000;

                Log.d(TAG, new StringBuilder(64)
                        .append(timeStr)
                        .append('\t')
                        .append(time)
                        .append("  ")
                        .append(src.startsWith(hostToken) ? "UE" : "ePDG")
                        .append(" -> ")
                        .append(dst.startsWith(hostToken) ? "UE" : "ePDG")
                        .append(" len = ")
                        .append(len).toString()
                );

                // 決策樹分析
                StateTracer.Direction dir = dst.startsWith(hostToken) ? StateTracer.Direction.DOWNWARD
                        : StateTracer.Direction.UPWARD;
                tracer.nextState(dir, len);

                // Here analyze the time stamp of each pkt
                ++pktNumber;

//                if (index <= 100) {
//                    farray[index] = ftime;
//                    avg_time = ((avg_time*(index-1)) + ftime)/index;
//                    index ++;
//                }
//                else {
//                    tmp = farray[replace_index];
//                    avg_time += ((ftime - farray[replace_index])/100);
//                    farray[replace_index] = ftime;
//                    replace_index = (replace_index+1)%101;
//                    if (replace_index == 0) {
//                        replace_index = 1;
//                    }
//                }
                // Log.d( "avg", "avg: "+ avg_time + " ftime: "  + ftime + " , index: " + (index-1) + " , replace_index: " + replace_index);
            }
            Log.d(TAG, "-- Captured Packets end --");

        } catch (NoSuchElementException e) {}


        if (BuildConfig.DEBUG) {
            // 監聽 Stderr
            Log.d(TAG, "-- Error Channel --");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String info;
                while ((info = br.readLine()) != null) {
                    Log.d(TAG, info);
                }

            } catch (IOException e) {}
            Log.d(TAG, "-- Error Channel ends --");
        }
        timer.cancel();
        Log.d(TAG, "Stopped.");
    }

    public boolean isStopped() {
        return looping;
    }

    public void stop() {
        // TODO - 繼承 Thread 複寫 destroy 方法
        looping = false;
        if(process != null){
            process.destroy();
        }
    }
}
