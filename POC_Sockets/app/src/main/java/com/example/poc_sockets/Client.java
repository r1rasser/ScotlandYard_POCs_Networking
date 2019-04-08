package com.example.poc_sockets;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends Thread{

    private int port;
    private InetAddress address;
    private Socket socket;
    private Activity activity;

    public int getPort() {
        return port;
    }

    Client(Socket socket, Activity activity) {
        this.socket = socket;
        this.activity = activity;
    }

    Client(Activity activity)  {
        this.activity = activity;
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

    public void connect() {
        try {
            socket = new Socket(address, port);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class Sender extends Thread {
        private String str;

        Sender(String str) {
            this.str = str;
        }

        public void run() {
            if (socket != null) {
                try {
                    DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                    writer.writeBytes(str + '\n');
                    writer.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public void send(String str) {
        Sender sender = new Sender(str);
        sender.start();
    }

     public void run() {
        while (true) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final String ret = reader.readLine();

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        TextView tv = activity.findViewById(R.id.response);
                        tv.setText(ret);
                    }
                });
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
     }

}
