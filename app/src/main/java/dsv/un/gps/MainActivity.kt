package dsv.un.gps

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.math.RoundingMode
import java.text.DecimalFormat
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.sql.Timestamp
import java.util.UUID


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var format: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    lateinit var lat : TextView
    lateinit var lon : TextView
    lateinit var alt : TextView
    lateinit var dat : TextView
    lateinit var get : Button
    lateinit var send : Button
    lateinit var stop : Button
    lateinit var num : EditText
    lateinit var sendip : Button
    var pressed = false
    lateinit var stringToActivity : String
    var response = ""
    val df = DecimalFormat("#.##")
    lateinit var service: Button
    lateinit var socket: BluetoothSocket
    private val mainScope = CoroutineScope(Dispatchers.Main)


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),0)
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        lat = findViewById(R.id.latv)
        lon = findViewById(R.id.lonv)
        alt = findViewById(R.id.altv)
        dat = findViewById(R.id.datev)
        get = findViewById<Button>(R.id.btngetv)
        send = findViewById<Button>(R.id.btnsendv)
        num = findViewById(R.id.numv)
        sendip = findViewById<Button>(R.id.btnip)
        service = findViewById<Button>(R.id.serviceButton)
        stop = findViewById<Button>(R.id.StopService)
        stop.setOnClickListener(this)
        get.setOnClickListener(this)
        send.setOnClickListener(this)
        sendip.setOnClickListener(this)
        service.setOnClickListener(this)
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
        }






    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick(v: View?) {


        when(v?.id){
            R.id.btngetv ->{
                pressed = true
                checkGPSPermissions()
            }
            R.id.btnsendv ->{
                lifecycleScope.launch(Dispatchers.IO) {
                    checkBluetoothDevices()
                    Thread.sleep(1000)
                    response = sendSerialToBluetooth()
                    println("Response=$response")
                    val bytes = response.split(" ")
                    val A = bytes[3].toInt(radix = 16)
                    val B = bytes[4].toInt(radix = 16)
                    println("A = $A, B = $B")
                    val data = ((256*A)+B)/4
                    val obdData = data.toString()
                    println(obdData)
                }
                Toast.makeText(this, "Bluetooth Connected?",Toast.LENGTH_SHORT).show()
            }
            R.id.btnip -> {
                if(pressed) {
                    val ipIntent = Intent(this, sendToInternet::class.java)
                    ipIntent.putExtra("Coordinates",stringToActivity)
                    startActivity(ipIntent)
                }
            }
            R.id.serviceButton -> {
                requestBackgroundLocation()
                startService(Intent(this,BackgroundTracking::class.java))
            }
            R.id.StopService -> {
                response = sendSerialToBluetooth()
                Toast.makeText(this, response,Toast.LENGTH_SHORT).show()
            }
        }

    }

    @SuppressLint("MissingPermission")
    suspend fun checkBluetoothDevices(){
        val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, discoverDevicesIntent)
        bluetoothAdapter.startDiscovery()
        val device = bluetoothAdapter.getRemoteDevice("00:10:CC:4F:36:03") //F0:03:8C:C7:55:6A pc Juan, 00:10:CC:4F:36:03 ELM,327
        val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        socket = device.createRfcommSocketToServiceRecord(MY_UUID)
        socket.connect()
        println("Success")
    }
    private fun sendSerialToBluetooth(): String {
        var inputStream: InputStream = socket.inputStream
        var outputStream: OutputStream = socket.outputStream
        val command = "01 0C"
        outputStream.write(command.toByteArray())
        val buffer = ByteArray(1024)
        val bytesRead = inputStream.read(buffer)
        var response = buffer.copyOf(bytesRead).toString(Charsets.UTF_8)
        inputStream= socket.inputStream
        outputStream = socket.outputStream
        outputStream.write(command.toByteArray())
        val bytesRead2 = inputStream.read(buffer)
        response = buffer.copyOf(bytesRead2).toString(Charsets.UTF_8)
        socket.close()
        return response
    }

    private fun checkGPSPermissions() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
            // Permission is not granted yet
            requestPermission()
        }else{
            // Granted permission
            getCoordinates()

        }

    }

    private fun checkSMSPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            print(1)
            requestSMSPermission()

        } else {
            print(0)
            sendSMS()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getCoordinates() {
        val coordinates = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,CancellationTokenSource().token)
        coordinates.addOnSuccessListener {
            if(it!=null){
                latitude = it.latitude
                longitude = it.longitude
                altitude = it.altitude
                val date = it.time
                format = Timestamp(date).toString()
                lat.text = "Latitude: $latitude"
                lon.text = "Longitude: $longitude"
                alt.text = "Altitude: $altitude"
                dat.text = "Date: ${format.toString()}"
                stringToActivity = "$latitude,$longitude,${df.format(altitude)},${date.toString()}"
                Toast.makeText(applicationContext, "Coordinates Obtained", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun sendSMS() {
        val tlf = num.text.toString()
        df.roundingMode = RoundingMode.DOWN
        val ms = "Enviado a $tlf"
        val sms = "Latitude: ${df.format(latitude)} Longitude: ${df.format(longitude)} Altitude: ${df.format(altitude)} Date: ${format.toString()} "
        val sms2 = "Lat: ${df.format(latitude)}, Long: ${df.format(longitude)}, Alt: ${df.format(altitude)}, ${format.toString()}"
        var smsSend = SmsManager.getDefault()
        smsSend.sendTextMessage(tlf,null,sms2,null,null)
        Toast.makeText(applicationContext, sms, Toast.LENGTH_LONG).show()
        Toast.makeText(applicationContext, ms, Toast.LENGTH_LONG).show()
    }

    private fun requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(applicationContext, "This app can't work without accurate location permission1", Toast.LENGTH_LONG).show()
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
                Toast.makeText(applicationContext, "This app can't work without accurate location permission2", Toast.LENGTH_LONG).show()
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 777)
            }
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 777)
        }
    }

    private fun requestSMSPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.SEND_SMS)){
            Toast.makeText(applicationContext, "This app needs SMS permissions", Toast.LENGTH_LONG).show()
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)){
                Toast.makeText(applicationContext, "This app needs SMS permissions", Toast.LENGTH_LONG).show()
            } else {
                 ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS),111)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS),111)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocation() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
            Toast.makeText(applicationContext, "This app needs Background location permissions", Toast.LENGTH_LONG).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),222)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 777){ //permission of interest
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCoordinates()
            }
        }
        if(requestCode == 111){
            if(grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                sendSMS()
            }
        }
        if(requestCode == 222) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCoordinates()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }


}
