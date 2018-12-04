package com.project2.server;

import com.project2.app.Calendar;
import com.project2.client.Client;
import com.project2.client.Message;

import java.util.*;
import java.io.*;


public class Local {

    public static int k = 0; // next entry
    public static ArrayList<Appointment> schedule = new ArrayList<>();
    public static ArrayList<Event> log = new ArrayList<>();
    public static int state = -1; // 1: wait promise, 3: waiting ack, 6: waiting maxK, -1: none
    public static Queue<Event> msg_set = new LinkedList<>();
    private String siteId;

    private String maxPrepare;
    private String pNum;
    private Event pVal;
    private String accNum;
    private Event accVal;
    private int count;

    public Local(String siteId_) {
        try {
            FileInputStream saveFile = new FileInputStream("state.sav");
            ObjectInputStream restore = new ObjectInputStream(saveFile);

            this.k = (Integer) restore.readObject();
            this.schedule = (ArrayList<Appointment>) restore.readObject();
            this.log = (ArrayList<Event>) restore.readObject();
            this.siteId = siteId_;
            restore.close();

        } catch (Exception i) {
            maxPrepare = null;
            accNum = null;
            accVal = null;
            pVal = null;
            pNum = null;
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
    public void myView(){
        Collections.sort(schedule, Appointment.timeComparator);
        for (Appointment a : schedule) {
            for (String p : a.getParticipants()){
                if (p.equals(siteId)) {
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

    private void updateSchedule(Event e){
        if (e.getOp().equals("Schedule")){
            schedule.add(e.getAppointment());
        }
        else if (e.getOp().equals("Cancel")){
            schedule.remove(e.getAppointment());
        }
    }

    private boolean checkConflicts(Appointment upcoming){
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

    private int mCompare(String m1, String m2) {
        if (m1.length() == m2.length() && m1.compareTo(m2) == 0) return 0;
        else if (m1.length()>m2.length() ||
                (m1.length() == m2.length() && m1.compareTo(m2) > 0))
            return 1;
        else return -1;
    }

    private String prepareM() {
        return "ab";
    }

    public void sanity_check() {
        state = 6;
        count = 0;
        new Client(siteId).bcast(5, "-1", new Event(k,
                null, null, null,null, null, null));
    }

    void start_paxos(Event proposal) {
        state = 1;
        count = 0;
        pVal = proposal;
        pNum = prepareM();
        new Client(siteId).bcast(0, pNum, pVal);
    }

    void end_paxos() {
        state = -1;
        count = 0;
        accVal = null;
        accNum = null;
        pVal = null;
        pNum = null;
    }

    void handle_msg(Message msg) {
        int port = Calendar.phonebook.get(msg.getSenderId());
        // On receive prepare(m)
        if (msg.getOp() == 0) {
            if (msg.getV().getK() > k && state != 6) sanity_check();
            if (mCompare(msg.getM(), maxPrepare) > 0) {
                maxPrepare = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        1, siteId, accNum, accVal));
            }
        }
        // On receive accept(accNum, accVal)
        else if (msg.getOp() == 2) {
            if (msg.getV().getK() > k && state != 6) sanity_check();
            if (mCompare(msg.getM(), maxPrepare) >= 0) {
                maxPrepare = msg.getM();
                accVal = msg.getV();
                accNum = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        3, siteId, accNum, accVal));
            }
        }
        // On receive commit(v)
        else if (msg.getOp() == 4) {
            updateLog(msg.getV());
            // check holes
            if (msg.getV().getK() > k && state != 6) sanity_check();
            // If I'm it's the entry I am working on && I am not filling holes, I quit
            if (msg.getV().getK() == k && state != 6) {
                if (!msg_set.isEmpty()) sanity_check();
                else end_paxos();
            }
        }
        // On receive check maxK
        else if (msg.getOp() == 5) {
            // TODO: send values from his K to my K
            if (msg.getV().getK() > k && state != 6) sanity_check();
        }
        // On receive waiting messages
        else if (msg.getOp() == state) {
            count++;
            if (state == 6) {
                // TODO: Fill holes
                if (count >= Calendar.majority) {
                    if (msg_set.isEmpty()) state = -1;
                    else start_paxos(msg_set.remove());
                }
            }
            if (state == 1) {
                if (msg.getV() != null && !msg.getV().equals(pVal)) {
                    System.out.println("Unable to "+pVal.getOp()+" meeting "+pVal.getAppointment().getName()+".");
                    pVal = msg.getV();
                }
                if (count >= Calendar.majority) {
                    state = 3;
                    count = 0;
                    new Client(siteId).bcast(2, pNum, pVal);
                }
            }
            if (state == 3 && count >= Calendar.majority) {
                new Client(siteId).bcast(4, pNum, pVal);
                end_paxos();
                if (!msg_set.isEmpty()) sanity_check();
            }
        }
    }

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
