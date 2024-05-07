package dsv.un.gps

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dsv.un.gps.RunService.Companion.addressStore
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.DecimalFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


class BackgroundTracking: Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val df = DecimalFormat("#.##")
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var payload = ""
    lateinit var socket: BluetoothSocket
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var response: String
    lateinit var obdData: String
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(applicationContext, "The Service is Called", Toast.LENGTH_LONG).show()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        start()
        scope.launch {
            sendToStations()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ForegroundServiceType")
    private fun start() {
        val notification = NotificationCompat.Builder(this,"channel").setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Servicio esta Corriendo")
            .setContentText("GPS")
            .build()
        startForeground(1, notification)
    }


    private suspend fun sendToStations() {
        val server1 = addressStore.getAddress1()
        val server2 = addressStore.getAddress2()
            while(true) {
                obdData = "No Data"
                try {
                    checkBluetoothDevices()
                    Thread.sleep(1000)
                    response = sendSerialToBluetooth()
                    println("Response=$response")
                    val bytes = response.split(" ")
                    val A = bytes[2].toInt(radix = 16)
                    val B = bytes[3].toInt(radix = 16)
                    println("A = $A, B = $B")
                    val data = ((256*A)+B)/4
                    obdData = data.toString()
                } catch (e: IOException){
                    println("OBD Not found")
                }
                println(obdData)
                mainScope.launch{getCoordinates(fusedLocationProviderClient)}
                sendUdpMessage(payload, server1, server2)
                Thread.sleep(10000)
            }

    }

    private suspend fun sendToStationsNoOBD() {
        val server1 = addressStore.getAddress1()
        val server2 = addressStore.getAddress2()
        while(true) {
            mainScope.launch{getCoordinates(fusedLocationProviderClient)}
            sendUdpMessage(payload, server1, server2)
            Thread.sleep(10000)
        }

    }

    suspend fun sendUdpMessage(payload: String,server1:String,server2: String) {
        val fullAddress1 = server1.split(":")
        val fullAddress2 = server2.split(":")
        val ip1 = InetAddress.getByName(fullAddress1[0])
        val ip2 = InetAddress.getByName(fullAddress2[0])
        val socketAddress1 = InetSocketAddress(ip1,fullAddress1[1].toInt())
        val socketAddress2 = InetSocketAddress(ip2,fullAddress2[1].toInt())
        val socket = DatagramSocket()
        socket.broadcast = true
        val sendData = payload.toByteArray()
        socket.send(DatagramPacket(sendData,sendData.size,socketAddress1))
        socket.send(DatagramPacket(sendData,sendData.size,socketAddress2))
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCoordinates(fusedLocationProviderClient: FusedLocationProviderClient) {
        val truck = addressStore.getCar()
        val coordinates = fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token)
        coordinates.addOnSuccessListener {
            if(it!=null){
                val latitude = it.latitude
                val longitude = it.longitude
                val altitude = it.altitude
                val date = it.time
                payload = "$latitude,$longitude,${df.format(altitude)},${date.toString()},${obdData},${truck}"
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
        Thread.sleep(500)
        inputStream= socket.inputStream
        outputStream = socket.outputStream
        outputStream.write(command.toByteArray())
        val bytesRead2 = inputStream.read(buffer)
        response = buffer.copyOf(bytesRead2).toString(Charsets.UTF_8)
        socket.close()
        println("Recieved $response")
        println("Socket Closed")
        return response
    }
}