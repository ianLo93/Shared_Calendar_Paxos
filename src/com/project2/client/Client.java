package com.project2.client;

import com.project2.app.Calendar;
import com.project2.server.Event;
import com.project2.server.Local;

import java.util.*;

public class Client {

    private String siteId;

    public Client(String siteid_) {
        this.siteId = siteid_;
    }

    public Event parse_command(String command) {
        // Parse command
        String[] cmds = command.split(" ");
        // Error checking
        if (cmds.length < 1) {
            System.out.println("ERROR: Invalid Command");
            System.out.println("USAGE: <command> [<meeting_info>]");
            return null;
        }
        // Implement command "schedule", "cancel", "view", "myview", "log"
        if (cmds[0].equals("schedule")) {
            if (cmds.length != 6) {
                System.out.println("ERROR: Invalid Meeting Schedule");
                System.out.println("USAGE: schedule <name> <day> <start_time> " +
                        "<end_time> <participants>");
                return null;
            }
            if (cmds[2].equals("10/14/2018") && cmds[2].equals("10/15/2018") &&
                    cmds[2].equals("10/16/2018") && cmds[2].equals("10/17/2018") &&
                    cmds[2].equals("10/18/2018") && cmds[2].equals("10/19/2018") &&
                    cmds[2].equals("10/20/2018")) {
                System.out.println("DAY SCHEDULE ERROR: <day> Format Error");
                return null;
            }
            String[] participants = valid_users(cmds[5].split(","));
            if (participants.length == 0) {
                System.out.println("SCHEDULE ERROR: No Valid User Provided");
                return null;
            }
            return new Event(Local.k,  cmds[0], cmds[1], cmds[2], cmds[3], cmds[4], participants);
        } else if (cmds[0].equals("cancel")) {
            if (cmds.length != 2) {
                System.out.println("ERROR: Invalid Meeting Cancellation");
                System.out.println("USAGE: cancel <name>");
                return null;
            }
            return new Event(Local.k, cmds[0], cmds[1], null, null, null, null);
        }
//        else if (cmds[0].equals("quit")) {
//            if (cmds.length != 1) {
//                System.out.println("ERROR: Invalid Exit Command");
//                System.out.println("USAGE: quit");
//                return null;
//            }
//            return null;
//        } else if (cmds[0].equals("init")) {
//            if (cmds.length != 1) {
//                System.out.println("ERROR: Invalid Initialization Command");
//                System.out.println("USAGE: init");
//                return null;
//            }
//            return null;
//        }
        else {
            System.out.println("ERROR: Invalid Command");
            System.out.println("USAGE: <command> [<meeting_info>]");
            return null;
        }
    }

    public void bcast(int op, String m_, Event proposal) {
        for (Map.Entry<String, int[]> entry: Calendar.phonebook.entrySet()) {
            Sender sd = new Sender(new Message(op, siteId, m_, proposal),
                    entry.getKey(), entry.getValue()[1]);
            sd.start();
        }
    }

    public void sendTo(String siteid_, int port_, Message msg_) {
        Sender sd = new Sender(msg_, siteid_, port_);
        sd.start();
    }

    private String[] valid_users(String[] participants) {
        ArrayList<String> valid_part = new ArrayList<>();
        for (String p : participants) {
            if (Calendar.phonebook.containsKey(p)) valid_part.add(p);
        }
        String[] vp = new String[valid_part.size()];
        for (int i=0; i<valid_part.size(); i++) vp[i] = valid_part.get(i);
        return vp;
    }
}
