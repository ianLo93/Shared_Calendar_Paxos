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
    private static String winner = " ";
    private String siteId;

    private String maxPrepare;
    private String pNum;
    private Event pVal;
    private String accNum;
    private Event accVal;
    private int count;
    private int propose;

    @SuppressWarnings("unchecked")
    public Local(String siteId_, boolean dummy) {
        if (!dummy) {
            try {
                FileInputStream saveFile = new FileInputStream("state.sav");
                ObjectInputStream restore = new ObjectInputStream(saveFile);

                k = (Integer) restore.readObject();
                log = (ArrayList<Event>) restore.readObject();
                this.siteId = siteId_;
                this.accNum = (String) restore.readObject();
                this.accVal = (Event) restore.readObject();
                this.maxPrepare = (String) restore.readObject();
                this.propose = (Integer) restore.readObject();
                clearSite();
                restore.close();
                try {
                    saveFile = new FileInputStream("checkpoint.sav");
                    restore = new ObjectInputStream(saveFile);

                    schedule = (ArrayList<Appointment>) restore.readObject();
                    restore.close();


                } catch (Exception i) {
//                System.out.println("load checkpoint.sav failed");
                }

                int reconstructK = 5 * (k / 5);
                while (reconstructK < k) {
                    updateSchedule(log.get(reconstructK));
                    reconstructK++;
                }

                sanity_check();
                setTimer(2);

            } catch (Exception i) {
                init(siteId_);
                sanity_check();
                setTimer(2);
            }
        }
    }

    // Helper functions
    void myView(){
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

    void view(){
        Collections.sort(schedule, Appointment.timeComparator);
        for (Appointment a : schedule) System.out.println(a);
    }

    void viewLog(){
        for (Event e : log) System.out.println(e);
    }

    void updateLog(Event e){
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

    boolean checkValidity(Event e){
//        System.out.println("checking validity");
        if (e.getOp().equals("schedule")) return !checkConflicts(e.getAppointment());
        else return checkExist(e.getAppointment());
    }

    private boolean checkExist(Appointment a){
        for (Appointment a_: schedule){
            if (a_.getName().equals(a.getName())) return true;
        }
        return false;
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
            System.out.println(log.get(startK));
            plog.add(log.get(startK));
            startK++;
        }
        int port = Calendar.phonebook.get(msg.getSenderId())[1];
        Message sendMsg = new Message(6, siteId, null, null);
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

        return Integer.toString(propose) + pos_i;
    }

    public void sanity_check() {
        state = 6;
        count = 0;
        new Client(siteId).bcast(5, "-1", new Event(k,
                null, null, null, null, null, null));
    }

    private void start_paxos(Event proposal) {
        if (!checkValidity(proposal)) {
            System.out.println("Unable to "+proposal.getOp()+ " meeting "
                    +proposal.getAppointment().getName()+".");
            return ;
        }
        state = 1;
        count = 0;
        pVal = proposal;
        pNum = prepareM();
        proposal.setK(k);
        new Client(siteId).bcast(0, pNum, pVal);
    }

    private void accept_paxos() {
        state = 3;
        count = 0;
        new Client(siteId).bcast(2, pNum, pVal);
    }

    private void clearSite() {
        accVal = null;
        accNum = null;
        maxPrepare = "0";

//        pVal = null;
//        pNum = null;
    }

    class Acceptor extends Thread {

        private Message msg;

        Acceptor(Message msg_) {
            this.msg = msg_;
        }

        @Override
        public void run() {
            int port = Calendar.phonebook.get(msg.getSenderId())[1];
            // On receive prepare(m)
            if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 0) {
//                System.out.println("receiving prepare msg");
                if (mCompare(msg.getM(), maxPrepare) > 0) {
                    maxPrepare = msg.getM();
                    new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                            1, siteId, accNum, accVal));
                }
            }
            // On receive accept(accNum, accVal)
            else if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 2) {
//                System.out.println("receiving accept msg");
                if (mCompare(msg.getM(), maxPrepare) >= 0) {
                    maxPrepare = msg.getM();
                    accVal = msg.getV();
                    accNum = msg.getM();
                    new Client(siteId).sendTo(msg.getSenderId(), port, new Message(
                            3, siteId, accNum, accVal));
                }
            }
            // On receive check maxK
            else {
//                System.out.println("receiving sanity-check msg");
                sendHolesVal(msg);
            }
        }
    }

    void message_handler(Message msg) {
//        System.out.println("new message: "+msg.getOp()+" from "+msg.getSenderId());
        if (msg.getV() != null && msg.getV().getK() > k) {
            if (accVal != null && msg.getV().getK() > accVal.getK()) clearSite();
            if (state != 6) {
                sanity_check();
                setTimer(2);
            }
        }
        // Acceptor
        if (msg.getOp() == 0 || msg.getOp() == 2 || msg.getOp() == 5) {
            Acceptor acc = new Acceptor(msg);
            acc.setDaemon(true);
            acc.start();
        }
        // Learner: On receive commit(v)
        else if (msg.getV() != null && msg.getV().getK() >= k && msg.getOp() == 4) {
//            System.out.println("receiving commit msg");
            updateLog(msg.getV());
            clearSite();
            winner = msg.getSenderId();

            // If it's the entry I am working on
            if (msg.getV().getK() == k-1) {
                if (state != -1 && pVal != null && !msg.getV().sameAs(pVal)) {
                    System.out.println("Unable to " + pVal.getOp() + " meeting " +
                            pVal.getAppointment().getName() + ".");
                }
                if (state != 6) {
                    if (!msg_set.isEmpty()) {
                        sanity_check();
                        setTimer(2);
                    } else state = -1;
                }
            }
        }
        // Proposer: on receive waiting messages
        else if (msg.getOp() == state) {
//            System.out.println("message same as state");
            count++;
            // Waiting for maxK messages (sanity checking)
            if (state == 6) {
//                System.out.println("receiving holesValue msg");
                fixHoles(msg);
                if (count >= Calendar.majority) {
                    timer.cancel();
                    numRetries = 0;
                    if (msg_set.isEmpty()) state = -1;
                    else {
                        pVal = msg_set.remove();
                        pVal.setK(k);

                        if (winner != null && winner.equals(siteId)) {
                            pNum = "0";
                            accept_paxos();
                        }
                        else {
                            pNum = prepareM();
                            start_paxos(pVal);
                            setTimer(2);
                        }
                    }
                }
            }
            // Waiting for promise messages
            else if (state == 1) {
//                System.out.println("receiving promise msg");
                if (msg.getV() != null && !msg.getV().equals(pVal)) {
                    System.out.println("Unable to "+pVal.getOp()+" meeting "+pVal.getAppointment().getName()+".");
                    pVal = msg.getV();
                }
                if (count >= Calendar.majority) {
                    timer.cancel();
                    accept_paxos();
                    setTimer(2);
                }
            }
            // Waiting for ack messages greater than majority
            else if (state == 3 && count >= Calendar.majority) {
//                System.out.println("receiving ack msg");
                timer.cancel();
                new Client(siteId).bcast(4, pNum, pVal);
<<<<<<< HEAD
                System.out.println("Meeting "+pVal.getAppointment().getName()+" "+pVal.getOp()+"ed.");
=======
                if (pVal.getOp().equals("schedule"))
                    System.out.println("Meeting "+pVal.getAppointment().getName()+" scheduled.");
                else System.out.println("Meeting "+pVal.getAppointment().getName()+" cancelled.");
                state = -1;
>>>>>>> c1bcb1a122a5586215101de74e050d819e5bdc2c
                clearSite();
                state = -1;
                if (!msg_set.isEmpty()) {
                    pVal = msg_set.remove();
                    pVal.setK(k);
                    accept_paxos();
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
            save.writeObject(schedule);

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
        schedule.clear();
        log.clear();
        k = 0;
    }

    class Timeout extends TimerTask {
        @Override
        public void run() {
            if (numRetries < 2) {
                numRetries++;
                if (state == 6) sanity_check();
                else if (state != -1) start_paxos(pVal);
            } else {
                numRetries = 0;
                if (state != 6) {
//                    System.out.println(440);
                    System.out.println("450: Unable to " + pVal.getOp() + " meeting " +
                            pVal.getAppointment().getName() + ".");
                }
                if (!msg_set.isEmpty()) {
                    if (state == 6) start_paxos(msg_set.remove());
                    else sanity_check();
                } else {
                    timer.cancel();
                    clearSite();
                    state = -1;
                }

            }
        }
    }

}
