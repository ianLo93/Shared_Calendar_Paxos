package com.project2.client;

import com.project2.server.Event;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Message implements Serializable {

    private int operation; // -1: None, 0: prepare, 1: promise, 2: accept,
                            // 3: ack, 4: commit, 5: fill_holes, 6: recover_holes
    private String senderId;
    private String m; // Prepare number/accepted number
    private Event v; // Proposed value/accepted value
    private ArrayList<Event> plog;
    private int posK;

    public Message(int op_, String sender_, String m_, Event v_) {
        this.operation = op_;
        this.senderId = sender_;
        this.m = m_;
        this.v = v_;
    }

    // Getters
    public int getOp() {
        return operation;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getM() {
        return m;
    }

    public Event getV() {
        return v;
    }

    public ArrayList<Event> getPlog() {return this.plog;}

    public int getposK() {return this.posK;}

    public void setPlog(ArrayList<Event> plog_) {this.plog = plog_;}

    public void setposK(int k) {this.posK = k;}
}
