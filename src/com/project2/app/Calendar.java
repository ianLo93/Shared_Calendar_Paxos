package com.project2.app;

import java.util.*;
import java.io.*;
//import java.util.concurrent.Semaphore;

import com.project2.client.Client;
import com.project2.client.Message;
import com.project2.client.Sender;
import com.project2.server.Server;
import com.project2.server.Event;

public class Calendar {

    public static HashMap<String, Integer> phonebook = new HashMap<>();
//    public static Semaphore mutex = new Semaphore(1);

    public static void readFile(String path) {
        try {
            File file = new File(path);
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            String line;

            int index = 0;
            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                String[] socket_info = line.split(" ");
                String siteid = socket_info[0];
                int port = Integer.parseInt(socket_info[1]);
                phonebook.put(siteid, port);
                index++;
            }
        } catch (IOException i) {
            System.out.println(i);
            System.exit(1);
        }
    }

    public static void main(String args[]) {

        // Read system site infos and make phonebook
//        readFile("knownhosts_udp.txt");

        phonebook.put("localhost", 8000);

        if(args.length != 1 || !Calendar.phonebook.containsKey(args[0])){
            System.out.println("ERROR: Invalid Arguments");
            System.out.println("USAGE: ./a.java <site_id>");
            System.exit(1);
        }

        // Get port number (args[0] stands for site ID)
        int port = Calendar.phonebook.get(args[0]);

        Server server = new Server(port);
        server.setDaemon(true);
        server.start();
        Client client = new Client(args[0]);

        Scanner sc = new Scanner(System.in);
        String command;
        while (true) {
            try {
//                mutex.acquire();
//                if (!server.getStatus()) break;
                command = sc.nextLine();
                if (command.equals("view")) {
                    // TODO: print calendar
                } else if (command.equals("myview")) {
                    // TODO: print my calendar
                } else if (command.equals("log")) {
                    // TODO: print log
                } else {
                    Event proposal = client.parse_command(command);
//                    if (proposal != null) {
//                        // TODO: fill holes, do paxos
//
//                    }
                    String m = client.prepareM();
                    client.sendTo("localhost", 8000,
                            new Message(0, args[0], m, proposal));
                }
//                client.sendMsg(msg, args[0], port);
            } catch (Exception i) {
                System.out.println(i);
//                mutex.release();
                break;
            }
        }

    }

}