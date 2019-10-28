package id.tensky.bluetoothproject

import android.Manifest
import android.app.Activity
import android.app.ListActivity
import android.app.ProgressDialog
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.Tag
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_FINE_LOCATION = 2
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var connectThread: ConnectThread
    private val insecureUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    var bluetoothDeviceAddress = ""
    val TAG = "BluetoothProject"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
        }
        checkBluetooth()
        main_startSearch.setOnClickListener{
            bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
            val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(deviceBroadcastReceiver, intentFilter)
            registerReceiver(bondingBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        }
        main_startConnection.setOnClickListener{
            connectThread = ConnectThread()
            connectThread.start()
        }
    }

    private val deviceBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = p1?.action
            Log.d(TAG, "actions: $action $intent")
            if(action.equals(BluetoothDevice.ACTION_FOUND)){

                val device = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if(device!=null){

                    if(device.name == "BANTU_ARDUINO"){
                        device.createBond()
                        bluetoothDeviceAddress = device.address
                        bluetoothDevice = device
                    }
                    if(device.bondState == BluetoothDevice.BOND_BONDED){
                        Log.d(TAG, "Bluetooth Device found: ${device.name} + Bond Status: ${device.bondState}")
                    }
                    //Log.d(TAG, "Bluetooth Device found: ${device.name} + Bond Status: ${device.bondState}")
                }
            }
        }
    }

    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("BANTU", insecureUUID)
        }

        override fun run() {
            super.run()
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private val bondingBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = p1?.action
            Log.d(TAG, "actions: $action $intent")
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){

                val device = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if(device?.bondState == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "Bond Broadcast: Bonded")
                }
                if(device?.bondState == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "Bond Broadcast: Bonding")
                }
                if(device?.bondState == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "Bond Broadcast: NONE")
                }
            }
        }
    }

    fun checkBluetooth(){
        if(!bluetoothAdapter.isEnabled){
            val turnOn = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(turnOn, REQUEST_ENABLE_BT)
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                Toast.makeText(this, "Bluetooth sudah menyala", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Silahkan menyalakan bluetooth", Toast.LENGTH_SHORT).show()
                checkBluetooth()
            }
        }
    }

    inner class ConnectThread : Thread(){
        var mmSocket : BluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(insecureUUID)

        override fun run() {
            bluetoothAdapter.cancelDiscovery()
            mmSocket.use { socket ->

                try {
                    socket.connect()
                    Log.d(TAG, "ConnectThread: Thread Connect Success")

                }catch (e:Exception){
                    Log.d(TAG, "ConnectThread: socket connect failed: $e")
                    return
                }


                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    inner class Connect : AsyncTask<Void, Void, Void>() {
        lateinit var btSocket: BluetoothSocket
        override fun doInBackground(vararg p0: Void?): Void? {
            try {
                btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(insecureUUID)
                bluetoothAdapter.cancelDiscovery()
                btSocket.connect()
            }catch (e : Exception){

            }
            manageMyConnectedSocket(btSocket)
            return null
        }

    }


    inner class ConnectedThread(socket: BluetoothSocket) : Thread(){
        var bluetoothSocket : BluetoothSocket
        lateinit var inStream : InputStream
        lateinit var outStream : OutputStream
        init{
            Log.d(TAG, "ConnectedThread: Starting ")
            bluetoothSocket = socket
            try {
                Log.d(TAG, "ConnectedThread: try to get stream ")
                inStream = bluetoothSocket.inputStream
                outStream = bluetoothSocket.outputStream
            }catch (e : IOException){
                Log.d(TAG, "ConnectedThread: getting Stream failed")
            }
        }

        override fun run() {
            Log.d(TAG, "ConnectThread: Running")
            val buffer = ByteArray(1024)
            var bytes : Int
            while (true){
                try {
                    //Log.d(TAG, "ConnectedThread: trying to catch input")
                    bytes = inStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.d(TAG, "incoming message: $incomingMessage")
                }catch (e: IOException){
                    Log.d(TAG, "ConnectedThread: Catching failed")
                }
            }
        }
        fun close(){
            try {
                Log.d(TAG, "ConnectedThread: trying to close")
                bluetoothSocket.close()
            }catch (e: IOException){
                Log.d(TAG, "ConnectedThread: close failed ${e.stackTrace}")
            }
        }
    }

    @Synchronized
    fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket){
        val connectedThread = ConnectedThread(bluetoothSocket)
        connectedThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceBroadcastReceiver)
        unregisterReceiver(bondingBroadcastReceiver)
    }

}

