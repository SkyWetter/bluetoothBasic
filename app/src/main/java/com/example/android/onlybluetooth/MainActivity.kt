/* Using SmoothBluetooth Library */


package com.example.android.onlybluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice


class MainActivity : AppCompatActivity() {
    private val tag = "MainActivityDebug"  //Tag for debug


    var mBluetoothAdapter : BluetoothAdapter? = null

    /**
     * BroadcastReceiver waits for incoming intent from the bluetooth adapter, and logs the current state
     */
    private val mBroadcastReceiver1 = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {         //Function onReceive is a default member function of the BroadcastReceiver class
            val action: String = intent.action
            //When discovery finds a device
            if(action == BluetoothAdapter.ACTION_STATE_CHANGED){
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)

                when(state){
                    BluetoothAdapter.STATE_OFF->{ Log.d(tag,"onReceive: STATE OFF")}
                    BluetoothAdapter.STATE_TURNING_OFF->{Log.d(tag,"mBroadcastReceiver1: STATE TURNING OFF")}
                    BluetoothAdapter.STATE_ON->{Log.d(tag,"mBroadcastReceiver1: STATE ON")}
                    BluetoothAdapter.STATE_TURNING_ON->{Log.d(tag,"mBroadcastReceiver1: STATE TURNING ON")}

                }
            }
        }


    }


    override fun onDestroy(){
        Log.d(tag,"onDestroy: called.")
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
    }


    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        var button = findViewById<Button>(R.id.btnONOFF)

        //Gets this phones default adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        btnONOFF.setOnClickListener{

                Log.d(tag, "onClick: enabling/disabling bluetooth.")
                enableDisableBT()

        }
    }

    fun enableDisableBT(){
        if(mBluetoothAdapter == null){
            Log.d(tag,"enableDisableBT: Does not have BT capabilities")
        }
        if(!mBluetoothAdapter!!.isEnabled){
            Log.d(tag,"enableDisableBT: enabling BT.")
            var enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBTIntent)

            /** Filter That intercepts and log changes to your bluetooth status */
            var BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1,BTIntent)
        }
        if(mBluetoothAdapter!!.isEnabled){
            mBluetoothAdapter!!.disable()

            var BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1,BTIntent)

        }
    }

}
