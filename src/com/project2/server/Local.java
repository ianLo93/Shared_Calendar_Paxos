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
    private static Timer timer; // Set timeout periods
    private static int numRetries = 0;
    private String siteId;

    private String maxPrepare;
    private String pNum;
    private Event pVal;
    private String accNum;
    private Event accVal;
    private int count;
    private int propose;

    public Local(String siteId_) {
        try {
            FileInputStream saveFile = new FileInputStream("state.sav");
            ObjectInputStream restore = new ObjectInputStream(saveFile);

            this.k = (Integer) restore.readObject();
            this.log = (ArrayList<Event>) restore.readObject();
            this.siteId = siteId_;
            this.accNum = (String) restore.readObject();
            this.accVal = (Event) restore.readObject();
            this.maxPrepare = (String) restore.readObject();
            this.propose = (Integer) restore.readObject();

            // TODO: WRITE & READ ACCNUM, ACCVAL
            restore.close();

            try{
                saveFile = new FileInputStream("checkpoint.sav");
                restore = new ObjectInputStream(saveFile);

                this.schedule = (ArrayList<Appointment>) restore.readObject();
                restore.close();
            }
            catch (Exception i){
                System.out.println("load checkpoint.sav failed");
            }

            int reconstructK = 5*(k/5)+1;
            while (reconstructK <= k){
                updateSchedule(log.get(reconstructK));
            }

            sanity_check();
            setTimer(2);

        } catch (Exception i) {
            init(siteId_);
//            maxPrepare = "0";
//            accNum = null;
//            accVal = null;
//            pVal = null;
//            pNum = null;
//            propose = 1;
//            siteId = siteId_;

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
            saveState();
        }
    }

    private void updateSchedule(Event e){
        if (e.getOp().equals("schedule")) { schedule.add(e.getAppointment()); }
        else {
            if (checkExist(e.getAppointment())) schedule.remove(e.getAppointment());
        }
    }

    public boolean checkValidity(Event e){
        System.out.println("checking validity");
        if (e.getOp().equals("schedule")) return !checkConflicts(e.getAppointment());
        else return checkExist(e.getAppointment());
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
        int port = Calendar.phonebook.get(msg.getSenderId())[1];
        Message sendMsg = new Message(6, siteId, null, new Event(
                k, null, null, null, null, null, null));
        sendMsg.setPlog(plog);
        new Client(siteId).sendTo(msg.getSenderId(), port, sendMsg);
    }

    private boolean checkConflicts(Appointment upcoming){
        if (schedule.contains(upcoming)) return true;

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

    public void setTimer(int seconds) {
        timer = new Timer();
        timer.schedule(new Timeout(), seconds*1000, seconds*1000);
    }

    private String prepareM() {
        propose += 1;
        String pos_i = Integer.toString(Calendar.phonebook.get(siteId)[0]);
        while (Integer.toString(Calendar.index).length() > pos_i.length()) pos_i = "0"+pos_i;
        if (maxPrepare != null) {
            while((Integer.toString(propose) + pos_i).compareTo(maxPrepare) < 0){
                propose++;
            }
        }

        return Integer.toString(propose) +pos_i;

    }

    public void sanity_check() {
        state = 6;
        count = 0;
        new Client(siteId).bcast(5, "-1", new Event(k,
                null, null, null, null, null, null));
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
//        System.out.println("new message: "+msg.getOp());
        int port = Calendar.phonebook.get(msg.getSenderId())[1];
        if (msg.getV() != null && msg.getV().getK() > k) {
            if (accVal != null && msg.getV().getK() > accVal.getK()) end_paxos();
            if (state != 6) {
                sanity_check();
                setTimer(2);
            }
        }
        // On receive prepare(m)
        if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 0) {
            System.out.println("receiving prepare msg");
            if (mCompare(msg.getM(), maxPrepare) > 0) {
//                System.out.println(msg);
                maxPrepare = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        1, siteId, accNum, accVal));
            }
        }
        // On receive accept(accNum, accVal)
        else if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 2) {
            System.out.println("receiving accept msg");
            if (mCompare(msg.getM(), maxPrepare) >= 0) {
                maxPrepare = msg.getM();
                accVal = msg.getV();
                accNum = msg.getM();
                new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                        3, siteId, accNum, accVal));
            }
        }
        // On receive commit(v)
        else if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 4) {
            System.out.println("receiving commit msg");
            updateLog(msg.getV());
            // If it's the entry I am working on
            if (msg.getV().getK() == k-1) {
                if (state != -1 && pVal != null && !msg.getV().equals(pVal)) {
//                    System.out.println(243);
                    System.out.println("Unable to " + pVal.getOp() + " meeting " +
                            pVal.getAppointment().getName() + ".");
                }
                end_paxos();
                if (!msg_set.isEmpty()) {
                    sanity_check();
                    setTimer(2);
                }
            }
        }
        // On receive check maxK
        else if (msg.getOp() == 5) {
            System.out.println("receiving sanity-check msg");
            sendHolesVal(msg);
        }
        // On receive waiting messages
        else if (msg.getOp() == state) {
            System.out.println("message same as state");
            count++;
            // Waiting for maxK messages (sanity checking)
            if (state == 6) {
                System.out.println("receiving holesValue msg");
                fixHoles(msg);
                if (count >= Calendar.majority) {
                    timer.cancel();
                    numRetries = 0;
                    if (msg_set.isEmpty()) state = -1;
                    else start_paxos(msg_set.remove());
                }
            }
            // Waiting for promise messages
            else if (state == 1) {
                System.out.println("receiving promise msg");
                if (msg.getV() != null && !msg.getV().equals(pVal)) {
                    System.out.println(274);
                    System.out.println("Unable to "+pVal.getOp()+" meeting "+pVal.getAppointment().getName()+".");
                    pVal = msg.getV();
                }
                if (count >= Calendar.majority) {
                    timer.cancel();
                    numRetries = 0;
                    state = 3;
                    count = 0;
                    new Client(siteId).bcast(2, pNum, pVal);
                    setTimer(2);
                }
            }
            // Waiting for ack messages
            else if (state == 3 && count >= Calendar.majority) {
                System.out.println("receiving ack msg");
                timer.cancel();
                new Client(siteId).bcast(4, pNum, pVal);
                end_paxos();
                if (!msg_set.isEmpty()) {
                    sanity_check();
                    setTimer(2);
                }
            }
        }
        else return;
    }

    private void saveCheckPoint(){
        try {
            FileOutputStream saveFile = new FileOutputStream("checkpoint.sav");
            ObjectOutputStream save = new ObjectOutputStream(saveFile);

            save.writeObject(k);
            save.writeObject(schedule);
            save.writeObject(log);
            save.close();
        } catch (IOException i) {
            System.out.println("save checkpoint failed");
            System.out.println(i);
        }
    }

    private void saveState(){
        if (k % 5 == 0) saveCheckPoint();
        try {
            FileOutputStream saveFile = new FileOutputStream("state.sav");
            ObjectOutputStream save = new ObjectOutputStream(saveFile);

            save.writeObject(k);
            save.writeObject(log);
            save.writeObject(accNum);
            save.writeObject(accVal);
            save.writeObject(maxPrepare);
            save.writeObject(propose);
            save.close();
        } catch (IOException i) {
            System.out.println("save state failed");
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
    public void init(String siteid_) {
        maxPrepare = "0";
        accNum = null;
        accVal = null;
        pVal = null;
        pNum = null;
        propose = 1;
        siteId = siteid_;
        schedule = new ArrayList<Appointment>();
        log = new ArrayList<Event>();
        k = 0;
    }

    class Timeout extends TimerTask {
        @Override
        public void run() {
            if (numRetries < 2) {
                numRetries++;
                if (state == 6) sanity_check();
                else start_paxos(pVal);
            } else {
                numRetries = 0;
                end_paxos();
                timer.cancel();
                if (!msg_set.isEmpty()) {
                    sanity_check();
                    setTimer(2);
                }
                if (state != 6) {
                    System.out.println(362);
                    System.out.println("Unable to " + pVal.getOp() + " meeting " +
                            pVal.getAppointment().getName() + ".");
                }
            }
        }
    }

}
