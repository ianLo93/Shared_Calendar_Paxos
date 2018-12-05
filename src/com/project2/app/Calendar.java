package com.project2.app;

import java.util.*;
import java.io.*;
//import java.util.concurrent.Semaphore;

import com.project2.client.Client;
import com.project2.client.Message;
import com.project2.server.Local;
import com.project2.server.Server;
import com.project2.server.Event;

public class Calendar {

    public static HashMap<String, int[]> phonebook = new HashMap<>();
    public static int majority;
    public static int index = 0;
//    public static Semaphore mutex = new Semaphore(1);

    public static void readFile(String path) {
        try {
            File file = new File(path);
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            String line;


            while ((line = buffer.readLine()) != null) {
                line = line.trim();
                String[] socket_info = line.split(" ");
                String siteid = socket_info[0];
                int port = Integer.parseInt(socket_info[1]);
                phonebook.put(siteid, new int[]{index, port});
                index++;
            }
            index--;
        } catch (IOException i) {
            System.out.println(i);
            System.exit(1);
        }
    }

    public static void main(String args[]) {

        // Read system site infos and make phonebook
//        readFile("knownhosts_udp.txt");

        phonebook.put("localhost", new int[]{1, 8000});
        majority = phonebook.size()/2+1;

        if(args.length != 1 || !Calendar.phonebook.containsKey(args[0])){
            System.out.println("ERROR: Invalid Arguments");
            System.out.println("USAGE: ./a.java <site_id>");
            System.exit(1);
        }

        // Get port number (args[0] stands for site ID)
        int port = Calendar.phonebook.get(args[0])[1];

        Server server = new Server(args[0], port);
        server.setDaemon(true);
        server.start();
        Client client = new Client(args[0]);
        Local local = new Local(args[0]);

        Scanner sc = new Scanner(System.in);
        String command;
        while (true) {
            try {
//                mutex.acquire();
//                if (!server.getStatus()) break;
                command = sc.nextLine();
                if (command.equals("view")) {
                    local.view();
                } else if (command.equals("myview")) {
                    local.myView();
                } else if (command.equals("log")) {
                    local.viewLog();
                } else {
                    Event proposal = client.parse_command(command);
                    if (proposal != null) {
                        if (local.state == -1) {
                            local.sanity_check();
                            local.setTimer(2);
                        }
                        local.msg_set.add(proposal);
                    }
                }
            } catch (Exception i) {
                System.out.println(i);
//                mutex.release();
                break;
            }
        }
    }
}
