package com.project2.server;

import java.io.*;

public class Event implements Serializable {

    private int k; // Entry number
    private String op; // Schedule/Cancel
    private Appointment meeting;

    public Event(int k_, String op_, String name, String day, String start, String end, String[] participants) {
        this.k = k_;
        this.op = op_;
        this.meeting = new Appointment(name, day, start, end, participants);
    }

    public String getOp() { return op; }
    public int getK() { return k; }
    public Appointment getAppointment() { return meeting; }

    @Override
    public String toString(){
        StringBuilder m = new StringBuilder(op + " "+meeting);
        return m.toString();
    }

}
