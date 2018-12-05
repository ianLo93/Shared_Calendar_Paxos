package com.project2.client;

import java.io.*;
import java.net.*;

public class Sender extends Thread {

    private Message msg;
    private DatagramSocket clientSocket;
    private String receiver;
    private int recvPort;
    private byte[] buf;

    Sender(Message msg_, String siteid_, int port_) {
        try {
            this.msg = msg_;
            this.receiver = siteid_;
            this.recvPort = port_;
            this.clientSocket = new DatagramSocket();
        } catch (SocketException s) {
            System.out.println(s);
        }
    }

    @Override
    public void run() {
        try {
            // Write object on stream buffer
            ByteArrayOutputStream byOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byOut);
            objOut.writeObject(msg);
            // Close output stream
            objOut.close();
            // Output to buffer
            buf = byOut.toByteArray();

            // Put obj into datagram packet and mark the address and port
            InetAddress addr = InetAddress.getByName(receiver);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, recvPort);

            // Send packets
            clientSocket.send(packet);
//            clientSocket.close();
        }
        catch (IOException i) {
            System.out.println(i);
        }
    }
}
