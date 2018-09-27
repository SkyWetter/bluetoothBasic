//Using Bluetooth tutorial:
// https://www.youtube.com/watch?v=Fz_GT7VGGaQ
//





package com.example.android.onlybluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //Accept thread mInsecureAcceptThread is initiated and waits for something to try to connect to it

    private class AcceptThread extends Thread {

        //The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){    //Runs on separate
            BluetoothServerSocket tmp = null;

            //Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG,"AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            }catch(IOException e){
                Log.e(TAG,"AcceptThread: IOException: " + e.getMessage());
            }

            mmServerSocket = tmp;
        }


        //Run thread begins, accept thread will 'hold' in this method until something connects to our socket
        public void run(){
            Log.d(TAG,"run: AcceptThread Running.");

            BluetoothSocket socket = null;
            try {
            //This is a blocking call and will only return on a successful connection or an exception
            Log.d(TAG, "run: RFCOM server socket start...");


                socket = mmServerSocket.accept();  //<<--- This is where thread waits until connection is made
            } catch(IOException e) {
                Log.e(TAG,"AcceptThread: IOException: " + e.getMessage());
            }

            if(socket != null){  //If we have something in the socket
                connected(socket,mmDevice);  //connect to s
            }

            Log.i(TAG,"End mAcceptThread");
        }

        public void cancel(){
            Log.d(TAG,"cancel: Canceling AcceptThread.");
            try{
                mmServerSocket.close();
            }catch (IOException e){
                Log.e(TAG,"cancel: Close of AcceptThread ServerSocket failed." + e.getMessage());
            }
        }

    }
}
