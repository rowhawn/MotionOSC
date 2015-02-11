package com.oscviz.rohan.motionosc;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class MainSensorActivity extends ActionBarActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensorAccel;
    private Sensor mSensorGyro;
    private float mSensorX;
    private float mSensorY;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private boolean sendFlag = false;

    private OSCPortOut sender;
    InetAddress remoteIP;
    int remotePort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sensor);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size()!=0){
            mSensorAccel = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            mSensorManager.registerListener(this, mSensorAccel, SensorManager.SENSOR_DELAY_GAME);
        }
        if(mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size()!=0){
            mSensorGyro = mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
            mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_GAME);
        }

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

        try {
            remoteIP = InetAddress.getByName("192.168.1.3");
        } catch (UnknownHostException e){
            e.printStackTrace();
        }
        remotePort = 1337;
        try {
            sender = new OSCPortOut(remoteIP, remotePort);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSensorChanged(SensorEvent event){

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            TextView sensorMessage = (TextView) findViewById(R.id.accel_sensor_value);
            OSCMessageSender senderTask = new OSCMessageSender();
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    mSensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    mSensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    mSensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    mSensorY = -event.values[0];
                    break;
            }
            float normalizedX = mSensorX / mSensorAccel.getMaximumRange();
            float normalizedY = mSensorY / mSensorAccel.getMaximumRange();
            sensorMessage.setText(String.valueOf(normalizedX) + ", " + String.valueOf(normalizedY));
            if (sendFlag) {
                senderTask.execute(normalizedX, normalizedY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    public void sendOscMessage(View view){
        new OSCMessageSender().execute(0.666f);
    }

    public void sendGyroReading(View view){
        sendFlag = !sendFlag;
        if (sendFlag){
            ((TextView)findViewById(R.id.sending_gyro_text)).setText(R.string.sending_gyro_text);
        } else {
            ((TextView)findViewById(R.id.sending_gyro_text)).setText(R.string.not_sending_gyro_text);
        }
    }

    public void sendTimingMessage(View view){
        Long time= System.currentTimeMillis();
        new OSCTimingSender().execute(time);
        // TODO implement send current time, calculate latency in ms
    }

    public void setConnectionDetails(View view){
        String ipInput = ((EditText) findViewById(R.id.ip_input)).getText().toString();
        String portInput = ((EditText) findViewById(R.id.port_input)).getText().toString();
        try {
            remoteIP = InetAddress.getByName(ipInput);
        } catch (UnknownHostException e){
            e.printStackTrace();
        }
        remotePort = Integer.parseInt(portInput);
        try {
            sender = new OSCPortOut(remoteIP, remotePort);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private class OSCMessageSender extends AsyncTask<Float, Void, Boolean> {

        protected Boolean doInBackground(Float... params) {
//            String ipAddress = ((EditText)findViewById(R.id.ip_input)).getText();
//            EditText port = (EditText)findViewById(R.id.port_input);

            String address1 = "/body";
            List<Object> args = new ArrayList<Object>();
            for (Float param : params) {
                args.add(param);
            }
            OSCMessage msg = new OSCMessage(address1, args);
            boolean success = false;
            try {
                sender.send(msg);
                success = true;
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
            return success;
        }

        protected void onPostExecute(Boolean result) {
//            if (result){
//                System.out.println("send was success");
//            } else {
//                System.out.println("unable to send");
//            }
        }
    }

    private class OSCTimingSender extends AsyncTask<Long, Void, Boolean> {

        protected Boolean doInBackground(Long... params) {
//            String ipAddress = ((EditText)findViewById(R.id.ip_input)).getText();
//            EditText port = (EditText)findViewById(R.id.port_input);

            String address1 = "/bodytiming";
            List<Object> args = new ArrayList<Object>();
            for (Long param : params) {
                args.add(param);
            }
            OSCMessage msg = new OSCMessage(address1, args);
            boolean success = false;
            try {
                sender.send(msg);
                success = true;
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
            return success;
        }

        protected void onPostExecute(Boolean result) {
//            if (result){
//                System.out.println("send was success");
//            } else {
//                System.out.println("unable to send");
//            }
        }
    }
}
