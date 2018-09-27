//Using Bluetooth tutorial:
// https://www.youtube.com/watch?v=Fz_GT7VGGaQ
//





package com.example.android.onlybluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

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
        //RUN METHOD IS AUTOMATICALLY CALLED IN A GIVEN THREAD, NO NEED TO BE MANUALLY CALLED
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

    /**
     * This thread runs while attempting to make an outgoing connection with a device. It runs straight through;
     * the connection either succeeds or fails
     */
    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket; //Socket for the incoming connected device?

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG,"ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread");

            //Get a BluetoothSocket for a connection with the
            //given Bluetooth device

            try{
                Log.d(TAG,"ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                    +MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch(IOException e){
                Log.e(TAG,"ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            //Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            try{
            //This is a blocking call and will only return one
            //a successful connection or an exception
            mmSocket.connect();
            } catch (IOException e){
                //Close the socket
            }
        }
    }
}
