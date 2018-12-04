package com.project2.server;

import com.project2.app.Calendar;
import com.project2.client.*;

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
    private String siteId;

    public Local() {
        try {
            FileInputStream saveFile = new FileInputStream("state.sav");
            ObjectInputStream restore = new ObjectInputStream(saveFile);

            this.k = (Integer) restore.readObject();
            this.schedule = (ArrayList<Appointment>) restore.readObject();
            this.log = (ArrayList<Event>) restore.readObject();
            restore.close();

            int reconstructK = 5*(k/5)+1;
            while (reconstructK <= k){
                updateSchedule(log.get(reconstructK));
            }
            this.state = 5;

        } catch (Exception i) {
            maxPrepare = null;
            accNum = null;
            accVal = null;
            this.state = 5;
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

    public int getState() { return this.state; }

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

    public void viewLog(){
        for (Event e : log) System.out.println(e);
    }

    public void updateLog(Event e){
        while (log.size() <= e.getK()){
            log.add(null);
        }
        log.set(e.getK(), e);
        while (k < log.size() && log.get(k) != null) {
            updateSchedule(log.get(k));
            k++;
            writeCheckPoint();
        }


    }

    private void updateSchedule(Event e){
        if (e.getOp().equals("Schedule")){
            schedule.add(e.getAppointment());
        }
        else if (e.getOp().equals("Cancel")){
            if (checkExist(e.getAppointment()))
                schedule.remove(e.getAppointment());
            else
                System.out.println("Unable to cancel meeting"+e.getAppointment().getName());
        }
    }

    private boolean checkExist(Appointment a){
        if (schedule.contains(a)) return true;
        else return false;
    }



    private void fixHoles(Message msg){
        if (msg.getOp() != 6) return ;
        ArrayList<Event> events = msg.getPlog();
        if (events.size() == 0) return ;
        if (events.get(events.size()-1).getK() < k) return ;

        for (Event e : events){
            updateLog(e);
        }
    }

    private void sendHolesVal(Message msg){
        if (msg.getOp() != 5) return ;
        if (msg.getposK() < k){
            int startK = msg.getposK();
            ArrayList<Event> plog = new ArrayList<Event>();
            while (startK < k){
                plog.add(log.get(startK));
            }
            int port = Calendar.phonebook.get(msg.getSenderId());
            Message sendMsg = new Message(6, siteId, null, null);
            sendMsg.setPlog(plog);
            new Client(siteId).sendTo(msg.getSenderId(), port, sendMsg);
        }
        else {

        }
    }

    private boolean checkConflicts(Appointment upcoming){
        String [] participants = upcoming.getParticipants();
        HashSet<String> dict = new HashSet<>(Arrays.asList(participants));

        int[] occupiedTimes = new int[48];
        int s = parse_time(upcoming.getStartTime()), e = parse_time(upcoming.getEndTime());

        for (Appointment a : schedule) {
            if (!a.getDay().equals(upcoming.getDay())) continue;
            for (String p : a.getParticipants()){
                if (dict.contains(p)) {
                    int sm = parse_time(a.getStartTime()), em = parse_time(a.getEndTime());
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

    private void writeCheckPoint(){
        if (k % 5 != 0) return;
        try {
            FileOutputStream saveFile = new FileOutputStream("checkpoint.sav");
            ObjectOutputStream save = new ObjectOutputStream(saveFile);

            save.writeObject(k);
            save.writeObject(schedule);
            save.writeObject(log);
            save.close();
        } catch (IOException i) {
            System.out.println("save_state() failed");
            System.out.println(i);
        }
    }


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
