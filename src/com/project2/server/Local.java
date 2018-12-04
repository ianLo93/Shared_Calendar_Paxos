package com.project2.server;

import com.project2.client.Message;

import java.util.*;
import java.io.*;


public class Local {

    public static int k = 0; // next entry
    private static ArrayList<Appointment> schedule = new ArrayList<>();
    private static ArrayList<Event> log = new ArrayList<>();
    public static int state = -1;
    public static Queue<Event> msg_set = new LinkedList<>();
    private String maxPrepare;
    private String accNum;
    private Event accVal;

    public Local() {
        try {
            FileInputStream saveFile = new FileInputStream("state.sav");
            ObjectInputStream restore = new ObjectInputStream(saveFile);

            this.k = (Integer) restore.readObject();
            this.schedule = (ArrayList<Appointment>) restore.readObject();
            this.log = (ArrayList<Event>) restore.readObject();
            restore.close();

        } catch (Exception i) {
            maxPrepare = null;
            accNum = null;
            accVal = null;
        }

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
    public void myView(String myID){
        Collections.sort(schedule, Appointment.timeComparator);
        for (Appointment a : schedule) {
            for (String p : a.getParticipants()){
                if (p.equals(myID)) {
                    System.out.println(a);
                    break;
                }
            }
        }
    }

    public void view(){
        Collections.sort(schedule, Appointment.timeComparator);
        for (Appointment a : schedule) System.out.println(a);
    }

    public void add(){}

    public void viewLog(){
        for (Event e : log) System.out.println(e);
    }

    public void updateLog(Event e){
        log.add(e);

    }

    private void remakeSchedule() {
        // TODO: read log and construct schedule
        HashSet<Appointment> s = new HashSet<>();
        for (int i=log.size()-1; i >= 0; i--) {
            Event e = log.get(i);
            if (e.getOp().equals("Schedule")) s.add(e.getAppointment());
            else s.remove(e.getAppointment());
        }
        for (Appointment a: s) schedule.add(a);
    }
//    private void updateSchedule(Event e){
//        if (e.getOp().equals("Schedule")){
//            schedule.add(e.getAppointment());
//        }
//        else if (e.getOp().equals("Cancel")){
//            schedule.remove(e.getAppointment());
//        }
//    }

    private Boolean checkConflicts(Appointment upcoming){
        String [] participants = upcoming.getParticipants();
        HashSet<String> dict = new HashSet<>(Arrays.asList(participants));

        int[] occupiedTimes = new int[48];
        int s = parse_time(upcoming.getStartTime()), e = parse_time(upcoming.getEndTime());

        for (Appointment m : schedule) {
            if (!m.getDay().equals(upcoming.getDay())) continue;
            for (String p : m.getParticipants()){
                if (dict.contains(p)) {
                    int sm = parse_time(m.getStartTime()), em = parse_time(m.getEndTime());
                    for (int i = sm; i < em; i++) occupiedTimes[i] = 1;
                    break;
                }
            }
        }

        for (int i = s; i < e; i++) {
            if (occupiedTimes[i] == 1) return true;
        }
        return false;
    }

    private boolean mCompare(String m1, String m2) {
        return m1.length()>m2.length() ||
                (m1.length() == m2.length() && m1.compareTo(m2) > 0);
    }

    void handle_msg(Message msg) {

    }
//
//    private void writeCheckPoint(){
//        if (k % 5 != 0) return;
//        try {
//            FileOutputStream saveFile = new FileOutputStream("checkpoint.sav");
//            ObjectOutputStream save = new ObjectOutputStream(saveFile);
//
//            save.writeObject(k);
//            save.writeObject(schedule);
//            save.writeObject(log);
//            save.close();
//        } catch (IOException i) {
//            System.out.println("save_state() failed");
//            System.out.println(i);
//        }
//    }


    static int parse_time(String timestamp) {
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
