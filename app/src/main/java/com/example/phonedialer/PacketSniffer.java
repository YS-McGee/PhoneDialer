package com.example.phonedialer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PacketSniffer implements Runnable {

    private boolean looping;
    private Process process;

    private ExecutorService executorService;

    private final StateTracer tracer;
    private final String hostToken;

    private final MyService service;

    public PacketSniffer(String hostToken, MyService service) {
        this.service = service;
        looping = true;
        tracer = new StateTracer(8, 4);

        this.hostToken = hostToken;

        executorService = Executors.newSingleThreadExecutor();

        Log.d(TAG, "Host Token: "+hostToken);
    }

    private final String TAG = "sniff";
    private final String[] commands = {
            "su", "-c",
            "tcpdump", "-ttt", "-q", "-n", "-l", "udp", "port", "4500", "-i", "any"
            //,"-c 10"
            // su -c tcpdump -tttqnlS udp port 4500 -i any
    };

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

        Callable<Optional<Packet>> receivePacket = ()->{
            String timeStr;
            long time;

            String ip, src, dst, protocol;
            int len;

            while(scanner.hasNext()){
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

                // 解析時間
                timeStr = timeStr.substring(timeStr.lastIndexOf(':')+1);
                timeStr = timeStr.replace('.', '0');
                time = Long.valueOf(timeStr);
                time = time/10000000 + time%1000000;

                return Optional.of(new Packet(time, src, dst, len));
            }

            return Optional.empty();
        };

        long timeout = 5;
        Optional<Packet> packetOptional;
        Packet packet;

        // 輸出格式:
        //   TT IP XX.XX.XX.XX.PORT > XX.XX.XX.XX.PORT: UDP, length NN
        int counter = 0;
        String left, right;
        Boolean boo = true;
        while (scanner.hasNext()) {

            try{
                packetOptional = executorService.submit(receivePacket).get( timeout, TimeUnit.SECONDS);

            }catch ( TimeoutException e){
                Log.e(TAG, "Next Packet Timeout");
                service.createAlert();
                break;
            }catch ( ExecutionException e){
                Log.e(TAG, "Something went wrong.");
                break;
            }catch ( InterruptedException e){
                continue;
            }catch ( CancellationException e){
                break;
            }

            if (!packetOptional.isPresent()) break;
            else packet = packetOptional.get();

            StateTracer.Direction dir = packet.src.startsWith(hostToken) ? StateTracer.Direction.UPWARD
                    : StateTracer.Direction.DOWNWARD;

            Log.d(TAG, new StringBuilder(64)
                    .append(packet.time)
                    .append("  ")
                    .append(dir == StateTracer.Direction.UPWARD ? "UE" : "ePDG")
                    .append(" -> ")
                    .append(dir== StateTracer.Direction.DOWNWARD ? "UE" : "ePDG")
                    .append(" len = ")
                    .append(packet.length).toString()
            );


            if (dir == StateTracer.Direction.UPWARD ) {
                ++counter;
                if (counter > 15 && boo ) {
                    service.createAlert();
                    boo = false;
                }
            } else {
                counter = 0;
            }

            // 決策樹分析
            StateTracer.State old = tracer.getState();
            StateTracer.State next = tracer.nextState(dir, packet.length);

            if (old != next){
                service.updateStateInNotification(next.toString());
            }

            // Here analyze the time stamp of each pkt
//                ++pktNumber;

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
        scanner.close();

        Log.d(TAG, "-- Captured Packets end --");


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
//        timer.cancel();
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
