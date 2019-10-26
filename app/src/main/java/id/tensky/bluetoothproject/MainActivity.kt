package id.tensky.bluetoothproject

import android.app.Activity
import android.app.ListActivity
import android.app.ProgressDialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT = 1
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothDevice: BluetoothDevice
    private val insecureUUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    val bluetoothConnectionService = BluetoothConnectionService(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkBluetooth()
        main_startSearch.setOnClickListener{
            bluetoothAdapter.startDiscovery()
            val devices = bluetoothAdapter.bondedDevices
            devices.forEach {
                if(it.name == "BANTU"){
                    bluetoothDevice = it
                    Toast.makeText(this, "ADA BANTU", Toast.LENGTH_SHORT).show()
                }
            }
        }
        main_startConnection.setOnClickListener{
            bluetoothConnectionService.start()
            bluetoothConnectionService.startClient(bluetoothDevice, insecureUUID)
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
}

