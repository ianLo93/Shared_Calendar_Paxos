package com.project2.server;

import com.project2.client.Message;

import java.io.*;
import java.net.*;

public class Server extends Thread {
    private DatagramSocket serverSocket;
    private Local local;
    private boolean running;

    public Server(String siteid, int port_) {
        try {
            this.serverSocket = new DatagramSocket(port_);
            this.local = new Local(siteid, false);
            this.running = false;
        } catch (SocketException s) {
            System.out.println(s);
        }
    }

    public boolean getStatus() { return running; }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                // Create datagram packet holder and send received msg to buf
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);

                // Get byte data and send to object stream
                byte[] data = packet.getData();
                ByteArrayInputStream byIn = new ByteArrayInputStream(data);
                ObjectInputStream objIn = new ObjectInputStream(byIn);

                // Get the message
                Message recvMsg;
                try {
                    recvMsg = (Message) objIn.readObject();
                } catch (ClassNotFoundException c) {
                    System.out.println(c);
                    continue;
                }
                // Input stream close
                objIn.close();
                if (recvMsg.getOp() == 7) {
                    if (recvMsg.getM() == null && recvMsg.getV() != null) {// proposal

                        if (!local.checkValidity(recvMsg.getV())){
                            System.out.println("Unable to "+recvMsg.getV().getOp()+" meeting "+
                            recvMsg.getV().getAppointment().getName()+".");
                            continue;
                        }
                        if (local.state == -1){
                            local.sanity_check();
;                           local.setTimer(2);
                        }
                        local.msg_set.add(recvMsg.getV());
                    }
                    else if (recvMsg.getM().equals("view")){
                        local.view();
                    }
                    else if (recvMsg.getM().equals("myview")){
                        local.myView();
                    }
                    else if (recvMsg.getM().equals("log")){
                        local.viewLog();
                    }
                    else if (recvMsg.getM().equals("init")){
                        local.init(recvMsg.getSenderId());
                    }
                }
                 else {
                     local.message_handler(recvMsg);
                }
            }  catch (IOException i) {
                System.out.println(i);
                running = false;
                break;
            }
        }
    }
}
