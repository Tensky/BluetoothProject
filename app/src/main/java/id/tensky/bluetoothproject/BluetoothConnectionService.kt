package id.tensky.bluetoothproject

import android.app.ProgressDialog
import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class BluetoothConnectionService(private val context: Context) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val insecureUUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val TAG = "BLUETOOTHPROJECT"
    private lateinit var myDevice: BluetoothDevice
    private lateinit var myDeviceUUID : UUID
    private lateinit var progressDialog: ProgressDialog
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread : ConnectedThread? = null


    inner class AcceptThread : Thread() {
        private var serverSocket: BluetoothServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("BluetoothProject", insecureUUID)!!

        override fun run(){
            Log.d(TAG, "Thread is Running")
            val bluetoothSocket = serverSocket.accept()
            if(bluetoothSocket != null){
                connected(bluetoothSocket, myDevice)
            }
        }

        fun cancel(){
            Log.d(TAG, "Cancelling Thread ")
            serverSocket.close()
        }

    }

    inner class ConnectThread() : Thread(){
        private lateinit var bluetoothSocket : BluetoothSocket
        constructor(device : BluetoothDevice, uuid : UUID) : this(){
            Log.d(TAG, "Connect Thread: Start ")
            myDevice = device
            myDeviceUUID = uuid
        }

        override fun run() {
            try {
                bluetoothSocket = myDevice.createRfcommSocketToServiceRecord(myDeviceUUID)
                Log.d(TAG, "ConnectThread: create Rfcomm socket sucessful")
            }catch (e:Exception){
                Log.d(TAG, "ConnectThread: failed to create Rfcomm Socket :  $e")
                e.printStackTrace()
            }
            bluetoothAdapter?.cancelDiscovery()
            try {
                bluetoothSocket.connect()
                Log.d(TAG, "ConnectThread: bluetoothSocket Connect sucessful")
            }catch (e: IOException){
                Log.d(TAG, "ConnectThread: bluetoothSocket Connect failed. Closing : ${e.message}")
                bluetoothSocket.close()
            }
            connected(bluetoothSocket, myDevice)
        }
        fun cancel(){
            try {
                Log.d(TAG, "ConnectThread: cancelling connection, closing")
                bluetoothSocket.close()
            }catch (e : IOException){
                Log.d(TAG, "ConnectThread: Cancel Failed" + e.message)
            }
        }
    }

    inner class ConnectedThread() : Thread(){
        lateinit var bluetoothSocket : BluetoothSocket
        lateinit var inStream : InputStream
        lateinit var outStream : OutputStream
        constructor(socket: BluetoothSocket):this(){
            Log.d(TAG, "ConnectedThread: Starting ")
            bluetoothSocket = socket
            progressDialog.dismiss()
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
                    Log.d(TAG, "ConnectedThread: trying to catch input")
                    bytes = inStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.d(TAG, "incoming message: $incomingMessage")
                }catch (e: IOException){
                    Log.d(TAG, "ConnectedThread: Catching failed")
                }
            }
        }
        fun write(bytes : ByteArray){
            val message = String(bytes, Charset.defaultCharset())
            try {
                Log.d(TAG, "ConnectedThread: trying to write: $message")
                outStream.write(bytes)
            }catch (e: IOException){
                Log.d(TAG, "ConnectedThread: write failed ${e.stackTrace}")
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

    fun connected(socket: BluetoothSocket, device: BluetoothDevice){
        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()
    }

    @Synchronized fun start(){
        Log.d(TAG, "Synchronized start ")
//        if(connectThread != null){
//            connectThread!!.cancel()
//            connectThread = null
//        }
        if(acceptThread == null){
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
    }

    fun startClient(device : BluetoothDevice, uuid: UUID){
        Log.d(TAG, "StartClient ")
        progressDialog = ProgressDialog.show(context, "Bluetooth connectiong", "Please Wait", true)
        connectThread = ConnectThread(device, uuid)
        connectThread!!.start()
    }

}