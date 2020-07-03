package com.example.multipairingwithui;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.UUID;

public class bluetoothService extends Service {
    public static Context mContext;
    private BroadcastReceiver mReceiver;

    private Messenger mClient = null;
    //IBinder mBinder = new MyBinder();

    boolean IsConnect0 = false,
            IsConnect1 = false;

    BluetoothAdapter BA;
    BluetoothDevice B0,B1;

    ConnectThread BC0;
    ConnectThread BC1;

    //final String B0MA = "98:D3:71:FD:47:5A"; //Bluetooth0 MacAddress
    final String B0MA = "00:18:91:D8:36:42"; //Bluetooth0 MacAddress

    final String B1MA = "98:D3:51:FD:88:9A"; //Bluetooth1 MacAddress

    final String SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"; //SPP UUID
    final UUID SPP_UUID = UUID.fromString(SPP_UUID_STRING);

    public static final int DISCONNECT = 0;
    public static final int CONNECTING = 50;
    public static final int CONNECTED = 2;
    public static final int INPUTDATA = 9999;

    Deque<String> valuesX = new ArrayDeque<>(5);
    Deque<String> valuesY = new ArrayDeque<>(5);
    ArrayList<String> Data = new ArrayList<String>();



    // onCreate : adapter, 및 변수설정, onStartCommand : 연결, onBind : 송신,  left, right 함수 두개로 송신


//    public class MyBinder extends Binder{
//        bluetoothService getMyservice(){ return bluetoothService.this; }
//    }

    //////////////////////////////
    //onUnbind() 필요한지 직접 꺼봐야함.
    //////////////////////////////


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    //수신값(현재 필요없음)
    private final Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch(msg.what){
                case 1:
                    mClient = msg.replyTo;
                    break;
            }
            return false;
        }
    }));

    private void sendMsgToActivity(Message msg){
        try{
            mClient.send(msg);
        } catch (NullPointerException | RemoteException e) {}
    }

    public void disconnectRight(){
        // Message msg = Message.obtain(null,0,DISCONNECT);
        try{
            BC0.cancel();
            //sendMsgToActivity(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectLeft(){
        // Message msg = Message.obtain(null,1,DISCONNECT);
        try{
            BC1.cancel();
            //sendMsgToActivity(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(){
        BA = BluetoothAdapter.getDefaultAdapter();
        B0 = BA.getRemoteDevice(B0MA);
        B1 = BA.getRemoteDevice(B1MA);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver,filter);

        mContext = this;
        super.onCreate();
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case 1:
                    break;
            }
        }
    };

    public void reconnectRight(){
        //if(!IsConnect0)//
        //{
        BC0 = new ConnectThread(B0,0);
        BC0.start();
        //}
    }
    public void reconnectLeft(){
        //if(!IsConnect1)
        //{
        BC1 = new ConnectThread(B1,1);
        BC1.start();
        //}
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BT SERVICE", "SERVICE STARTED");
        if(!IsConnect0)
        {
            BC0 = new ConnectThread(B0,0);
            BC0.start();
        }
        if(!IsConnect1)
        {
            BC1 = new ConnectThread(B1,1);
            BC1.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy(){

        if(BC0 != null)
        {
            try {
                BC0.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(BC1 != null)
        {
            try {
                BC1.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }


    class ConnectThread extends Thread{

        BluetoothDevice BD;
        BluetoothSocket BS;

        int bluetooth_index;

        ConnectedThread connectedThread;

        ConnectThread(BluetoothDevice device , int index){
            BD = device;
            bluetooth_index = index;
        }

        @Override
        public void run() {
            try {
                Message msg = new Message();
                if(!IsConnect0)
                {
                    msg.what = bluetooth_index;
                    msg.arg1 = CONNECTING;
                    sendMsgToActivity(msg);
                }
                else if(!IsConnect1)
                {
                    msg.what = bluetooth_index;
                    msg.arg1 = CONNECTING;
                    sendMsgToActivity(msg);
                }

                BS = BD.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                BS.connect();

                connectedThread = new ConnectedThread(BS, bluetooth_index);
                connectedThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    cancel();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if(connectedThread != null){
                    connectedThread.cancel();
                }
            }
        }

        public void cancel() throws IOException {

            if(connectedThread != null){
                connectedThread.cancel();
            }
            if(BS != null)
            {
                try{
                    BS.close();
                    BS = null;
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(int arg){
            Message m = new Message();
            m.what = bluetooth_index;
            m.arg1 = CONNECTING;

            //handler.sendMessage(m);
        }
    }

    //connected bluetooth - communication
    class ConnectedThread extends Thread{

        InputStream in = null;

        int bluetooth_index;

        boolean is =false;

        public ConnectedThread(BluetoothSocket bluetoothsocket, int index) {
            bluetooth_index = index;

            try {
                in = bluetoothsocket.getInputStream();

                is = true;

                if(bluetooth_index == 0) IsConnect0 = is;
                else IsConnect1 = is;

                sendMessage(CONNECTED);

            } catch (IOException e) {
                cancel();
            }
        }

        @Override
        public void run() {
            final sign signThread;

            signThread = new sign(mHandler);
            signThread.setDaemon(true);
            signThread.start();

            BufferedReader Buffer_in = new BufferedReader(new InputStreamReader(in));

            while (is){
                try {
                    String s = Buffer_in.readLine();

                    //if(IsConnect1 && IsConnect0 && s.length() >= 54){
                    System.out.println(s);
//                        sendMessage(INPUTDATA, s);
//
//                        if(bluetooth_index == 0){
//                            if(valuesX.size() < 5){
//                                valuesX.push(s);
//                            }
//                        }
//
//                        if(valuesX.size() == 5){
//                            for(int i = 0; i < 5; i++){
//                                Data.add(valuesX.pollFirst());
//                                Message msg = Message.obtain(null,0,Data.get(i));
//                                signThread.bringHandler.sendMessage(msg);
//                            }
//                            Data.clear();
//                            valuesX.clear();
//                        }
                    //}
                } catch (IOException e) { }
            }
        }

        public void sendMessage(int arg){
            Message m = new Message();

            m.what = bluetooth_index;
            m.arg1 = arg;

            sendMsgToActivity(m);
        }

        public void sendMessage(int arg, String s){
            Message m = new Message();

            m.what = bluetooth_index;
            m.arg1 = arg;
            m.obj = s;
            sendMsgToActivity(m);
        }

        public void cancel(){
            is = false;

            if(bluetooth_index == 0) IsConnect0 = is;
            else IsConnect1 = is;

            if(in != null){
                try {
                    in.close();
                    in=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sendMessage(DISCONNECT);
        }
    }
    public static class sign extends Thread{
        Handler bringHandler;
        Handler sendHandler;
        ArrayList<String[]> handData;
        Double[] lastCordinateX = new Double[]{0.00, 0.00, 0.00};
        double[] rightHandData = new double[6];
        double E = 0.0;
        double totalEnergy = 0.0;
        int count = 0;


        sign(Handler handler){
            sendHandler = handler;
        }

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();
            bringHandler = new Handler(){
                public void handleMessage(Message msg){
                    switch (msg.what){
                        case 0:
                            String[] arr = ((String)msg.obj).split(",");
                            for(int i = 0; i < 6; i++){
                                if(i >= 3){
                                    rightHandData[i] = Double.parseDouble(arr[i]) * 5;
                                    //5 를 Math에서 곱해보기
                                }
                                else{
                                    rightHandData[i] = Double.parseDouble(arr[i]);
                                }
                            }
                            E = Math.pow(rightHandData[3] - lastCordinateX[0],2) + Math.pow(rightHandData[4] - lastCordinateX[1],2) + Math.pow(rightHandData[5] - lastCordinateX[2],2) ;

                            for(int i = 0; i < 3; i++){
                                lastCordinateX[i] = rightHandData[i + 3];
                            }
                            System.out.println(E);
                            if(count < 5){
                                totalEnergy += E;
                            }
                            else{
                                //정욱이 말대로 Queue로 구현해야함. 그렇지 않으면 5개 합의 에너지를 구하고 다음 차례로 넘어갈 시 첫번째 원소를 찾을 수 없음. -> Queue로 구현.
                                //복잡도 pollFirst 와 push 할때 delay가 생기는지 확인 해야함.
                            }
                            break;
                    }
                    //send
                }
            };
            Looper.loop();
        }
    }

}