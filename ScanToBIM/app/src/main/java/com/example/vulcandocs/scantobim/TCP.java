package com.example.vulcandocs.scantobim;


import android.os.AsyncTask;
import android.util.Log;
import java.io.File;

public class TCP {
    private TCPClient mTcpClient;

    public void start() {
        // connect to the server

        new connectTask().execute("");
        sendFile();
    }

    public class connectTask extends AsyncTask<String, String, TCPClient> {
        @Override
        protected TCPClient doInBackground(String... message) {
            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                //this method calls the onProgressUpdate
                publishProgress(message);
                }
            });
            //mTcpClient.getIp();
            mTcpClient.run();
            return null;
        }
    }

    public void sendFile() {
        try{
            Thread.sleep(1000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        String path = "/storage/emulated/0/Documents/myScans";
        File[] files = new File(path).listFiles();
        System.out.println(files.length);
        if (mTcpClient != null) {
            mTcpClient.sendFile(files);
        }else{
            System.out.println("mTCPClient null");
        }

    }
}
