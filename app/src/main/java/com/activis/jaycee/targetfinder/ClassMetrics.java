package com.activis.jaycee.targetfinder;

import android.os.AsyncTask;
import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;

public class ClassMetrics
{
    private static final String TAG = ClassMetrics.class.getSimpleName();
    private static final String DELIMITER = "'";

    private WifiDataSend dataStreamer = null;

    private double timestamp, pitch;
    private double[] targetPosition = new double[3];
    private TangoPoseData tangoPose;

    public void writeWifi()
    {
        String wifiString = String.valueOf(timestamp) + DELIMITER
                + String.valueOf(tangoPose.translation[0]) + DELIMITER
                + String.valueOf(tangoPose.translation[1]) + DELIMITER
                + String.valueOf(tangoPose.translation[2]) + DELIMITER
                + String.valueOf(tangoPose.rotation[0]) + DELIMITER
                + String.valueOf(tangoPose.rotation[1]) + DELIMITER
                + String.valueOf(tangoPose.rotation[2]) + DELIMITER
                + String.valueOf(tangoPose.rotation[3]) + DELIMITER
                + String.valueOf(pitch)  + DELIMITER
                + String.valueOf(targetPosition[0]) + DELIMITER
                + String.valueOf(targetPosition[1]) + DELIMITER
                + String.valueOf(targetPosition[2]) + DELIMITER;

        /* WRITE TO WIFI PORT */
        if(dataStreamer == null || dataStreamer.getStatus() != AsyncTask.Status.RUNNING)
        {
            Log.d(TAG, "wifi transmitting");
            dataStreamer = new WifiDataSend();
            dataStreamer.execute(wifiString);
        }
    }

    public void updateTimeStamp(double timestamp)
    {
        this.timestamp = timestamp;
    }

    public void updateTangoPose(TangoPoseData tangoPose)
    {
        this.tangoPose = tangoPose;
    }

    public void updateTargetPosition(double[] targetPosition)
    {
        this.targetPosition = targetPosition;
    }

    public void updatePitch(double pitch)
    {
        this.pitch = pitch;
    }

    private class WifiDataSend extends AsyncTask<String, Void, Void>
    {
        private String serverIdAddress = "10.5.42.163";
        private int connectionPort = 6666;

        public WifiDataSend() { }

        @Override
        protected Void doInBackground(String... strings)
        {
            try
            {
                Socket socket = new Socket(serverIdAddress, connectionPort);
                OutputStream stream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(stream);

                int charsRead;
                int bufferLen = 1024;
                char[] tempBuffer = new char[bufferLen];

                BufferedReader bufferedReader = new BufferedReader(new StringReader(strings[0]));

                Log.d(TAG, "Writing to WiFi");
                while((charsRead = bufferedReader.read(tempBuffer, 0, bufferLen)) != -1)
                {
                    writer.print(tempBuffer);
                }
                writer.write("\n");

                writer.flush();
                writer.close();

                socket.close();
            }
            catch(IOException e)
            {
                Log.e(TAG, "Wifi write error: ", e);
            }

            return null;
        }
    }
}
