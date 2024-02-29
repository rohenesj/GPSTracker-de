package dsv.un.gps

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.math.RoundingMode
import java.text.DecimalFormat
import android.net.Uri
import android.telephony.SmsManager
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.sql.Timestamp
import java.util.Date
import kotlin.time.Duration.Companion.nanoseconds

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    //private lateinit var date: ZonedDateTime
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
    lateinit var num : EditText
    lateinit var sendip : Button
    var pressed = false
    lateinit var stringToActivity : String
    val df = DecimalFormat("#.##")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        lat = findViewById(R.id.latv)
        lon = findViewById(R.id.lonv)
        alt = findViewById(R.id.altv)
        dat = findViewById(R.id.datev)
        get = findViewById<Button>(R.id.btngetv)
        send = findViewById<Button>(R.id.btnsendv)
        num = findViewById(R.id.numv)
        sendip = findViewById<Button>(R.id.btnip)
        get.setOnClickListener(this)
        send.setOnClickListener(this)
        sendip.setOnClickListener(this)






    }

    override fun onClick(v: View?) {


        when(v?.id){
            R.id.btngetv ->{
                pressed = true
                checkGPSPermissions()
            }
            R.id.btnsendv ->{
                if(pressed) {
                    checkSMSPermissions()
                }
            }
            R.id.btnip -> {
                if(pressed) {
                    val ipIntent = Intent(this, sendToInternet::class.java)
                    ipIntent.putExtra("Coordinates",stringToActivity)
                    startActivity(ipIntent)
                }
            }
        }

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
        //val coordinates = fusedLocationProviderClient.lastLocation
        coordinates.addOnSuccessListener {
            if(it!=null){
                latitude = it.latitude
                longitude = it.longitude
                altitude = it.altitude
                val date = it.time
               // date = ZonedDateTime.now()
                format = Timestamp(date).toString()
                //format = DateTimeFormatter.ofPattern("EEE dd-MM-yyyy HH:mm:ss").format(date)
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
           // Toast.makeText(applicationContext, "This app needs SMS permissions", Toast.LENGTH_LONG).show()
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)){
               // Toast.makeText(applicationContext, "This app needs SMS permissions", Toast.LENGTH_LONG).show()
            } else {
                 ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS),111)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS),111)
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

    }



}
