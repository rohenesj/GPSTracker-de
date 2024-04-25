package dsv.un.gps

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
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


class BackgroundTracking: Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val df = DecimalFormat("#.##")
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var payload = ""



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(applicationContext, "The Service is Called", Toast.LENGTH_LONG).show()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
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
                mainScope.launch{getCoordinates(fusedLocationProviderClient)}
                sendUdpMessage(payload, server1, server2)
                println("STOP")
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
        val coordinates = fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token)
        coordinates.addOnSuccessListener {
            if(it!=null){
                val latitude = it.latitude
                val longitude = it.longitude
                val altitude = it.altitude
                val date = it.time
                payload = "$latitude,$longitude,${df.format(altitude)},${date.toString()}"
            }
        }
    }

}