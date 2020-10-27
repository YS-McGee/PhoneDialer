package com.example.phonedialer;

import android.util.Log;

import java.util.Arrays;

public class StateTracer{

    // Tag for Logcat
    private final String TAG = getClass().getSimpleName();

    // States during VoWifi conversation
    public enum State {
        NONE, INVITE1, TRYING2, SESSION3, PRACK4, OK5, RINGING6, PRACK7, OK8, OK9, ACK10, VOICE11, BYE12, OK13, ERROR14;
    }

    // Directions indicate the packet flow
    public enum Direction {
        UPWARD(1), DOWNWARD(-1);

        public final int value;
        Direction(int value){
            this.value = value;
        }
    }

    private final int maxStair;
    private int step;

    private final int maxErrorStair;
    private int errorStep;

    private final State[] floorArr = {State.NONE, State.RINGING6, State.VOICE11};
    private int floor;

    private boolean acquireBye;
    
    private State state;

    public StateTracer(int restoreStair, int errorStep){
        maxStair = restoreStair;
        maxErrorStair = errorStep;

        initial();
    }

    public void initial() {
        state = State.NONE;
        floor = 0;

        step = maxStair;
        errorStep = maxErrorStair;
    }

    public State nextState(Direction direction, int length){
        State next = predict(direction, length, state);

        Log.d(TAG, "State -> "+next);


        if (next == State.ERROR14){
            --errorStep;

            if (errorStep < 0){
                return State.ERROR14;
            }
        }else if (next != state)
        {
            errorStep = maxErrorStair;

            int target = Arrays.binarySearch(floorArr, next);

            if (target >= 0){
                floor = target;
            }

            state = next;
            resetStep();

        }else if (state != floorArr[floor]){
            if (step < 0){
                state = floorArr[floor];
                Log.d(TAG, "State timeout -> "+state);
            }
            --step;
        }

        return next;
    }

    public State predict(Direction dir, int len, State oldState){
        if (len < 0) return oldState;

        // One packet, one transition
        State next = oldState;

        switch (oldState) {
            case NONE:
                if (dir == Direction.UPWARD) {
                    if (800 < len && len < 1300)
                        next = State.INVITE1;
                } else {

                }
                break;
            case INVITE1:
                //Area1

                if (dir == Direction.UPWARD) {
                    // Error
                    if (800 < len && len < 1500){
                        return State.ERROR14;
                    }
                } else {
                    if (250 < len && len < 600) {
                        next = State.TRYING2;
                    } else if (1100 < len && len < 1500) {
                        next = State.SESSION3;
                    }
                }
                break;
            case TRYING2:
                if (dir == Direction.UPWARD) {
                    if (800 < len && len < 1500)
                        next = State.PRACK4;

                } else {
                    if (800 < len && len < 1500)
                        next = State.SESSION3;
                }
                break;
            case SESSION3:
                if (dir == Direction.UPWARD) {
                    if (800 < len && len < 1500)
                        next = State.PRACK4;
                } else {
                    if (500 < len && len < 1500)
                        next = State.OK5;

                }
                break;
            case PRACK4:
                if (dir == Direction.UPWARD) {
                    if (250 < len && len < 1500)
                        next = State.OK5;
                    else if (800 < len && len < 1500)
                        next = State.ERROR14;
                } else {
                    if (250 < len && len < 1500)
                        next = State.OK5;
                }
                break;
            case OK5:
                if (dir == Direction.UPWARD) {

                } else {
                    if (len < 250)
                        next = State.RINGING6;
                }
                break;
            case RINGING6:
                //Area2

                if (dir == Direction.UPWARD) {
                    if (800 < len && len < 1500)
                        next = State.PRACK7;
                } else {
                    if (400 < len && len < 1500)
                        next = State.PRACK7;
                }
                break;
            case PRACK7:
                if (dir == Direction.UPWARD) {
                    if (800 < len && len < 1500)
                        next = State.ERROR14;
                } else {
                    if (250 < len)
                        next = State.OK8;
                }
                break;
            case OK8:
                if (dir == Direction.UPWARD) {
                    if (400 < len && len < 1000)
                        next = State.ACK10;
                } else {
                    if (400 < len && len < 1000)
                        next = State.OK9;
                }
                break;
            case OK9:
                if (dir == Direction.UPWARD) {
                    if (400 < len && len < 1000)
                        next = State.ACK10;
                } else {
                }
                break;
            case ACK10:
                if (dir == Direction.UPWARD) {
                    if (250 < len && len < 400)
                        next = State.VOICE11;
                    else if (400 < len && len < 1500)
                        next = State.ERROR14;
                } else {
                    if (250 < len)
                        next = State.VOICE11;
                }
                break;
            case VOICE11:
                // Area3
                if (250 < len){
                    if (dir == Direction.UPWARD) acquireBye = true;
                    else acquireBye = false;
                    next = State.VOICE11;
                }

                break;
            case BYE12:
                if (250 < len){
                    if (acquireBye && dir == Direction.DOWNWARD){
                        next = State.OK13;
                    }else if (!acquireBye && dir == Direction.UPWARD){
                        next = State.OK13;
                    }else{
                        next = State.ERROR14;
                    }
                }

                break;
            case OK13:
                if (250 < len)
                    next = State.NONE;

                break;
        }

        return next;

    }


    public void resetStep(){
        step = maxStair;
    }

    public State getState(){
        return state;
    }


}

