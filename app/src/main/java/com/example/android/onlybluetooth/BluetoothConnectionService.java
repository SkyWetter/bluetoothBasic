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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionService {

    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "Info send";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;


    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();


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
        //Run method is a member function of Thread class, which activates automatically after thread creation
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

            Log.d(TAG,"run: ConnectThread connected.");
            } catch (IOException e){
                //Close the socket
                try{
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                }catch (IOException e1){
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID");
            }

            connected(mmSocket,mmDevice);
        }
        public void cancel(){
            try{
                Log.d(TAG,"cancel: Closing Client Socket.");
                mmSocket.close();
            }catch (IOException e){
                Log.e(TAG,"cancel: close() of mmSocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }



    /**
     * Start the chat service. Specifically, start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start(){
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection (so we start fresh)
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with other device's
     * AcceptThread
     */

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth"
                                ,"Please Wait...",true);

        mConnectThread = new ConnectThread(device,uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends  Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            try {
                mProgressDialog.dismiss();
            } catch(NullPointerException e){
                e.printStackTrace();
            }


            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch(IOException e){
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024]; //buffer store for the stream

            int bytes; //bytes returned from read()

            //Keep listening to the InputStream until an exception occurs
            while(true){
                //Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG,"InputStream: " + incomingMessage);
                } catch (IOException e){
                    Log.e(TAG,"write: Error reading inputStream. " + e.getMessage());
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"write: Writing to outputstream: " + text);
            try{
                mmOutStream.write(bytes);
            }catch (IOException e){
                Log.e(TAG,"write: Error writing to output stream. " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){

            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG,"connected: Starting. ");

        //Start the thread to manage the connection and perform transmissions

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     *
     */

    public void write(byte[] out){

        //Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        mConnectedThread.write(out);
    }
}
