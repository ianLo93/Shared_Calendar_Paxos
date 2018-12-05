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

            int reconstructK = 5*(k/5)+1;
            while (reconstructK <= k){
                updateSchedule(log.get(reconstructK));
            }
            sanity_check();

        } catch (Exception i) {
            maxPrepare = null;
            accNum = null;
            accVal = null;
            pVal = null;
            pNum = null;
            sanity_check();
        }


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
        while (log.size() <= e.getK()) log.add(null);
        log.set(e.getK(), e);
        while (k < log.size() && log.get(k) != null) {
            updateSchedule(log.get(k));
            k++;
            writeCheckPoint();
        }
    }

    private void updateSchedule(Event e){
        if (e.getOp().equals("Schedule")) { schedule.add(e.getAppointment()); }
        else {
            if (checkExist(e.getAppointment())) schedule.remove(e.getAppointment());
        }
    }

    private boolean checkExist(Appointment a){
        if (schedule.contains(a)) return true;
        else return false;
    }


    private void fixHoles(Message msg){
        ArrayList<Event> events = msg.getPlog();
        if (events.size() > 0 && events.get(events.size()-1).getK() >= k) {
            for (Event e : events)
                updateLog(e);
        }
    }

    private void sendHolesVal(Message msg) {
        int startK = msg.getV().getK();
        ArrayList<Event> plog = new ArrayList<>();
        while (startK < k) {
            plog.add(log.get(startK));
        }
        int port = Calendar.phonebook.get(msg.getSenderId());
        Message sendMsg = new Message(6, siteId, null, new Event(
                k, null, null, null, null, null, null));
        sendMsg.setPlog(plog);
        new Client(siteId).sendTo(msg.getSenderId(), port, sendMsg);
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
        if (state != 6) {
            state = 6;
            count = 0;
            new Client(siteId).bcast(5, "-1", new Event(k,
                    null, null, null, null, null, null));
        }
    }

    private void start_paxos(Event proposal) {
        state = 1;
        count = 0;
        pVal = proposal;
        pNum = prepareM();
        new Client(siteId).bcast(0, pNum, pVal);
    }

    private void end_paxos() {
        state = -1;
        accVal = null;
        accNum = null;
        pVal = null;
        pNum = null;
    }

    void handle_msg(Message msg) {
        int port = Calendar.phonebook.get(msg.getSenderId());
        if (msg.getV().getK() > k) {
            if (accVal != null && msg.getV().getK() > accVal.getK()) end_paxos();
            sanity_check();
        }
        // On receive prepare(m)
        if (msg.getV().getK() >= k && msg.getOp() == 0) {
            if (mCompare(msg.getM(), maxPrepare) > 0) {
                maxPrepare = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        1, siteId, accNum, accVal));
            }
        }
        // On receive accept(accNum, accVal)
        else if (msg.getV().getK() >= k && msg.getOp() == 2) {
            if (mCompare(msg.getM(), maxPrepare) >= 0) {
                maxPrepare = msg.getM();
                accVal = msg.getV();
                accNum = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        3, siteId, accNum, accVal));
            }
        }
        // On receive commit(v)
        else if (msg.getV().getK() >= k && msg.getOp() == 4) {
            updateLog(msg.getV());
            // If it's the entry I am working on
            if (msg.getV().getK() == k-1) {
                if (state != -1 && pVal != null && !msg.getV().equals(pVal))
                    System.out.println("Unable to "+pVal.getOp()+" meeting "+pVal.getAppointment().getName()+".");
                end_paxos();
                if (!msg_set.isEmpty()) sanity_check();
            }
        }
        // On receive check maxK
        else if (msg.getOp() == 5) {
            sendHolesVal(msg);
        }
        // On receive waiting messages
        else if (msg.getOp() == state) {
            count++;
            // Waiting for maxK messages (sanity checking)
            if (state == 6) {
                fixHoles(msg);
                if (count >= Calendar.majority) {
                    if (msg_set.isEmpty()) state = -1;
                    else start_paxos(msg_set.remove());
                }
            }
            // Waiting for promise messages
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
            // Waiting for ack messages
            if (state == 3 && count >= Calendar.majority) {
                new Client(siteId).bcast(4, pNum, pVal);
                end_paxos();
                if (!msg_set.isEmpty()) sanity_check();
            }
        }
        else return;
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
