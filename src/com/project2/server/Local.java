package com.project2.server;

import java.lang.reflect.Array;
import java.util.*;

public class Local {

    public static int k = 0; // next entry
    private static ArrayList<Appointment> schedule = new ArrayList<>();
    private static ArrayList<Event> log = new ArrayList<>();
    public static int wait = -1;
    private String maxPrepare;
    private String accNum;
    private Event accVal;

    public Local() {
        maxPrepare = null;
        accNum = null;
        accVal = null;
    }

    // Getters
    public String getMaxPrepare() {
        return maxPrepare;
    }

    public String getAccNum() {
        return accNum;
    }

    public Event getAccVal() {
        return accVal;
    }

    // Setters
    public void setMaxPrepare(String maxPrepare) {
        this.maxPrepare = maxPrepare;
    }

    public void setAccNum(String accNum) {
        this.accNum = accNum;
    }

    public void setAccVal(Event accVal) {
        this.accVal = accVal;
    }

    // Helper functions
    public void view(){
        Collections.sort(schedule, Appointment.timeComparator);
        for (Appointment a : schedule) System.out.println(a);
    }

    public void myView() {
        ArrayList<Appointment> meetings;

    }

    public void viewLog(){}

    public void add(){}

    public void remove(){}

    private void checkMemberAvailability(){}

    private ArrayList<Appointment> constructSchedule(){
        // TODO: readCopy

        // TODO: construct with log
    }

    private void writeCheckPoint(){}

    private ArrayList<Appointment> readCheckPoint(){ }

    private ArrayList<Integer> checkHoles(){}

    public static int parse_time(String timestamp) {
        String[] clocks = timestamp.split(":");
        if (clocks.length != 2) {
            System.out.println("ERROR: Invalid Time");
            System.out.println("USAGE: <hour[1:24]>:<minute>[00/30]");
            return -1;
        }
        try {
            int first = Integer.parseInt(clocks[0]);
            int second = Integer.parseInt(clocks[1]);
            return first * 2 + second/30;
        } catch (NumberFormatException n) {
            System.out.println("parse_time failed");
            return -1;
        }
    }



}
