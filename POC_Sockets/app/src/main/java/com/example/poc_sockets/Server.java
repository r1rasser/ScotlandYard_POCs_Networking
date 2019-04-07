package com.example.poc_sockets;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread{

    private int port;
    private InetAddress address;
    private InetAddress clientAdress;
    private int clientPort;
    private ServerSocket serverSocket;
    private Client client;

    public InetAddress getClientAdress() {
        return clientAdress;
    }

    public void setClientAdress(InetAddress clientAdress) {
        this.clientAdress = clientAdress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    private Activity activity;

    Server(Activity activity) {
        this.activity = activity;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        port = serverSocket.getLocalPort();
        start();
    }

    public void run() {
        try {
            // create ServerSocket using specified port

            while (true) {
                // block the call until connection is created and return
                // Socket object
                client = new Client(serverSocket.accept(), activity);
                new Thread(client).start();

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void send(String str) {
        client.send(str);
    }

    private class SocketServerThread extends Thread {

        @Override
        public void run() {

        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }
}
