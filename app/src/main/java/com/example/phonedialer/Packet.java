package com.example.phonedialer;

public class Packet {

    public final long time;
    public final String src;
    public final String dst;

    public final int length;

    public Packet(long time, String src, String dst, int length){
        this.time = time;
        this.src = src;
        this.dst = dst;
        this.length = length;


    }

}
