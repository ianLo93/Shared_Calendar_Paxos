package com.project2.server;

import java.util.*;

public class Local {

    public static int k = 0; // next entry
    public static ArrayList<Event> shedule = new ArrayList<>();
    public static ArrayList<Event> log = new ArrayList<>();
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
}
