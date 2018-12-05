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

    public void setK(int k_) { this.k = k_; }

    @Override
    public boolean equals(Object obj) {
        // checking if both the object references are
        // referring to the same object.
        if(this == obj)
            return true;

        if(obj == null || obj.getClass()!= this.getClass())
            return false;

        // type casting of the argument.
        Event e = (Event) obj;

        // comparing the state of argument with
        // the state of 'this' Object.
        return (e.k == this.k && e.op.equals(this.op) && e.meeting.equals(this.meeting));
    }

    @Override
    public String toString(){
        String m = op + " "+meeting;
        return m;
    }

}
