package com.project2.server;

import com.project2.client.Message;

import java.io.*;
import java.net.*;

public class Server extends Thread {
    private DatagramSocket serverSocket;
    private Local mySite;
    private boolean running;

    public Server(int port_) {
        try {
            this.serverSocket = new DatagramSocket(port_);
            this.mySite = new Local();
            this.running = false;
        } catch (SocketException s) {
            System.out.println(s);
        }
    }

    public boolean getStatus() { return running; }

    private void handle_msg(Message msg) {
        if (msg.getOp() == 0) {
//            if (msg.getM() > mySite.getMaxPrepare()) {
//                mySite.setMaxPrepare(msg.getM());
//
//            }
        }
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                // Create datagram packet holder and send received msg to buf
                byte[] buf = new byte[2048];
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

                // TODO: handle received message
                if (recvMsg.getV() == null) {
                    System.out.println("Copy the empty command");
                }
            }  catch (IOException i) {
                System.out.println(i);
                running = false;
                break;
            }
        }
    }
}
