/* Using SmoothBluetooth Library */


package com.example.android.onlybluetooth

import android.Manifest
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
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.*
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity(),AdapterView.OnItemClickListener {
    private val tag = "MainActivityDebug"  //Tag for debug

    var mBluetoothAdapter : BluetoothAdapter? = null
    var mBTDevices = mutableListOf<BluetoothDevice>();
    var mDeviceListAdapter: DeviceListAdapter? = null
    var lvNewDevices : ListView? = null
    var mBTDevice : BluetoothDevice? = null
    val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    var mBluetoothConnection : BluetoothConnectionService? = null

    /**
     * BroadcastReceivers waits for incoming intent from the bluetooth adapter, and logs the current state
     *
     * BR1 -- Logs BT Adapter On/Off state changes
     *
     * BR2 -- Logs BT Adapter Discoverability/Connection state changes
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

    private val mBroadcastReceiver2 = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {         //Function onReceive is a default member function of the BroadcastReceiver class
            val action: String = intent.action
            //When discovery finds a device
            if(action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED){
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)

                when(state){
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE->{ Log.d(tag,"mBroadcastReceiver2: Discoverability Enabled")}
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE->{Log.d(tag,"mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.")}
                    BluetoothAdapter.SCAN_MODE_NONE->{Log.d(tag,"mBroadcastReceiver2: Discoverability Disabled. Unable to receive connections.")}
                    BluetoothAdapter.STATE_CONNECTING->{Log.d(tag,"mBroadcastReceiver2: connecting... ")}
                    BluetoothAdapter.STATE_CONNECTED->{Log.d(tag,"mBroadcastReceiver2: Connected.")}

                }
            }
        }


    }

    private val mBroadcastReceiver3 = object : BroadcastReceiver(){

        override fun onReceive(context: Context, intent: Intent){
            val action: String = intent.action
            Log.d(tag,"onReceive: ACTION FOUND")
            //When discovery finds a device
            if(action == BluetoothDevice.ACTION_FOUND){
                var device : BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                mBTDevices.add(device)
                Log.d(tag,"onReceive: " + device.name + ": " + device.address)
                mDeviceListAdapter = DeviceListAdapter(context,R.layout.device_adapter_view,mBTDevices)
                lvNewDevices!!.adapter = mDeviceListAdapter

            }
        }
    }

    private val mBroadcastReceiver4 = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action

            if(action == BluetoothDevice.ACTION_BOND_STATE_CHANGED){
                var mDevice : BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                //3 cases
                //case 1: bonded already
                if(mDevice.bondState==BluetoothDevice.BOND_BONDED){
                    Log.d(tag,"BroadcastReceiver: BOND_BONDED")
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice
                }
                //case 2: creating a bond
                if(mDevice.bondState == BluetoothDevice.BOND_BONDING){
                    Log.d(tag,"BroadcastReceiver: BOND_BONDING")

                }
                //case 3: breaking a bond
                if(mDevice.bondState == BluetoothDevice.BOND_NONE){
                    Log.d(tag,"BroadcastReceiver: BOND_NONE")
                }
            }
        }
    }


    override fun onDestroy(){
        Log.d(tag,"onDestroy: called.")
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
        unregisterReceiver(mBroadcastReceiver2)
        unregisterReceiver(mBroadcastReceiver3)
        unregisterReceiver(mBroadcastReceiver4)
        //mBluetoothAdapter.cancelDiscovery()
    }


    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lvNewDevices = findViewById(R.id.lvNewDevices)
        val etSend:EditText = findViewById<EditText>(R.id.editText)


        //Broadcasts when bond state changes (ie: pairing)
        var filter : IntentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver4,filter)

        //Gets this phones default adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        lvNewDevices!!.onItemClickListener = this@MainActivity

        btnONOFF.setOnClickListener{

                Log.d(tag, "onClick: enabling/disabling bluetooth.")
                enableDisableBT()

        }

        btnEnableDisable_Discoverable.setOnClickListener{
                Log.d(tag,"onClick: btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.")

                var discoverableIntent : Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                startActivity(discoverableIntent)

                var intentFilter  = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
                registerReceiver(mBroadcastReceiver2,intentFilter)


        }

        btnDiscover.setOnClickListener{
            Log.d(tag,"btnDiscover: Looking for unpaired devices.")

            if(mBluetoothAdapter!!.isDiscovering){
                mBluetoothAdapter!!.cancelDiscovery()
                Log.d(tag,"btnDiscover: Cancelling discovery.")

                /** Required permission check for any API > Android Lollipop*/
                checkBTPermissions()

                mBluetoothAdapter!!.startDiscovery()
                var discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
            }
            if(!mBluetoothAdapter!!.isDiscovering){

                //Check BT permission in manifest
                checkBTPermissions()

                mBluetoothAdapter!!.startDiscovery()
                var discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
            }
        }

        btnSend.setOnClickListener{
            var bytes: ByteArray = etSend.getText().toString().toByteArray(Charset.defaultCharset())
            mBluetoothConnection!!.write(bytes)
        }
        /**
         * Start connection onClick -- app will fail and crash if it hasn't paired first
         */
        btnStartConnection.setOnClickListener{
            startBTConnection(mBTDevice!!,MY_UUID_INSECURE)
        }

    }

    /**
     * Starting chat service method
     */

    fun startBTConnection(device : BluetoothDevice, uuid : UUID){
        Log.d(tag,"startBTConnection: Initializing RFCOM Bluetooth Connection.")

        mBluetoothConnection!!.startClient(device,uuid)
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

    /**
     * This method is required for all deices running API23+
     * Android must programmatically check the permission for bluetooth.
     * Putting the proper permissions in the manifest is not enough.
     */

    private fun checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            var permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            if(permissionCheck !=0){

                this.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),1001)
            }else{
                Log.d(tag,"checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.")
            }
        }
    }


    override fun onItemClick(adapterView: AdapterView<*>, view : View,i: Int, l: Long){
        //first cancel discovery because its very memory intensive
        mBluetoothAdapter!!.cancelDiscovery()

        Log.d(tag,"onItemClick: You Clicked on a device.")
        var deviceName : String = mBTDevices[i].name
        var deviceAddress : String = mBTDevices[i].address

        Log.d(tag, "onItemClick: deviceName = $deviceName")
        Log.d(tag, "onItemClick: deviceName = $deviceAddress")

        //create the bond
        //NOTE: Requires API 17+
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(tag,"Trying to pair with $deviceName")
            mBTDevices[i].createBond()

            mBTDevice = mBTDevices[i]
            Log.d(tag,"COOL BOY")
            mBluetoothConnection =  BluetoothConnectionService(this@MainActivity)
        }

    }


}
