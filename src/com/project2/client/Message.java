package com.project2.client;

import com.project2.server.Event;

import java.io.*;
import java.util.ArrayList;

public class Message implements Serializable {

    private int operation; // 0: prepare, 1: promise, 2: accept, 3: ack, 4: commit,
    // 5: fill holes, 6: recover holes
    private ArrayList<Event> plog;

    private String senderId;
    private String m; // Prepare number/accepted number
    private Event v; // Proposed value/accepted value
    private int posK;

    public Message(int op_, String sender_, String m_, Event v_) {
        this.plog = new ArrayList<>();
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

    public ArrayList<Event> getPlog() { return this.plog; }

    public void setPlog(ArrayList<Event> plog_) { this.plog = plog_; }
}
