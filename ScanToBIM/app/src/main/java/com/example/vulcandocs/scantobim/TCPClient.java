package com.example.vulcandocs.scantobim;

import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    private String serverMessage;
    //your computer's public IP address

    //public static final String SERVERIP = "192.168.1.10"
    public static String SERVERIP;
    public static final int SERVERPORT = 4444;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    OutputStream out;
    BufferedOutputStream bos;
    BufferedReader in;
    DataOutputStream dos;

    public TCPClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }
    public TCPClient(){}

    public void getIp(String string){
        SERVERIP = string;
        System.out.println(SERVERIP);
    }

    public void sendFile(File[] files) {
        try {
            dos.writeInt(files.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (File file : files) {
            if (file.isDirectory())
                System.out.println("It's directory");
            else {
                try {
                    long length = file.length();
                    dos.writeLong(length);

                    String name = file.getName();
                    dos.writeUTF(name);

                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);

                    int theByte = 0;
                    while ((theByte = bis.read()) != -1) bos.write(theByte);

                    bis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("All files sent!");
    }

    public void run() {
        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);
            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVERPORT);
            try {
                //send the message to the server
                out = socket.getOutputStream();
                bos = new BufferedOutputStream(out);
                dos = new DataOutputStream(bos);
                Log.e("TCP Client", "C: Sent.");
                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    serverMessage = in.readLine();
                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(serverMessage);
                    }
                    serverMessage = null;
                }
                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }
        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    // class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}