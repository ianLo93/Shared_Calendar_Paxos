package com.project2.server;

import java.io.*;

public class Event implements Serializable {

    private int k; // Entry number
    private String op; // Schedule/Cancel
    private String name;
    private String day;
    private String start;
    private String end;
    private String[] participants;

    public Event(int k_, String op_, String name_, String day_, String start_,
                 String end_, String[] participants_) {
        this.k = k_;
        this.op = op_;
        this.name = name_;
        this.day = day_;
        this.start = start_;
        this.end = end_;
        this.participants = participants_;
    }

    public String getOp() { return op; }
    public String getName() { return name; }
    public String getDay() { return day; }
    public String getStartTime() { return start; }
    public String getEndTime() { return end; }
    public String[] getParticipants() { return participants; }
    public int getK() { return k; }

    @Override
    public String toString(){
        StringBuilder m = new StringBuilder(op + " " +name + " " + day+ " " + start + " " + end + " ");
        int i=0;
        for (; i<participants.length-1; i++){
            m.append(participants[i]+",");
        }
        m.append(participants[i]);
        return m.toString();
    }

}
