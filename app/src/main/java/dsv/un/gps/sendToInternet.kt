package dsv.un.gps

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import dsv.un.gps.RunService.Companion.addressStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class sendToInternet : AppCompatActivity() {

    lateinit var msg : EditText
    lateinit var sendMsg: Button
    lateinit var sendUdp: Button
    lateinit var sendBoth: Button
    lateinit var coordinates: String
    lateinit var saveServer1: Button
    lateinit var saveServer2: Button



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_to_internet)
        coordinates = intent.getStringExtra("Coordinates").toString()
        msg = findViewById(R.id.ipaddr)
        sendMsg = findViewById(R.id.btnsendnet)
        sendUdp = findViewById(R.id.btnudp)
        sendBoth = findViewById(R.id.bothprotocols)
        saveServer1 = findViewById(R.id.Server1Button)
        saveServer2 = findViewById(R.id.Server2Button)


        Toast.makeText(applicationContext, coordinates, Toast.LENGTH_LONG).show()

        sendMsg.setOnClickListener{
            val ip = msg.text.toString()
            val checkIp = addressStore.isItAValidIpv4(ip)
            if(checkIp) {
                lifecycleScope.launch(Dispatchers.IO) {
                    sendTcpMessage(ip)
                }
            } else {
                Toast.makeText(applicationContext, "Not a Valid Socket", Toast.LENGTH_LONG).show()
            }
        }

        sendUdp.setOnClickListener{
            val ip = msg.text.toString()
            val checkIp = addressStore.isItAValidIpv4(ip)
            if(checkIp) {
                lifecycleScope.launch(Dispatchers.IO) {
                    sendUdpMessage(ip)
                }
            } else {
                Toast.makeText(applicationContext, "Not a Valid Socket", Toast.LENGTH_LONG).show()
            }
        }

        sendBoth.setOnClickListener{
            val ip = msg.text.toString()
            val checkIp = addressStore.isItAValidIpv4(ip)
            if(checkIp) {
                lifecycleScope.launch(Dispatchers.IO) {
                    sendTcpMessage(ip)
                    sendUdpMessage(ip)
                }
            } else {
                Toast.makeText(applicationContext, "Not a Valid Socket", Toast.LENGTH_LONG).show()
            }
        }
        saveServer1.setOnClickListener{
            val ip = msg.text.toString()
            val checkIp = addressStore.isItAValidIpv4(ip)
            if(checkIp){
                val old = addressStore.getAddress1()
                addressStore.saveAddress1(ip)
                val new = addressStore.getAddress1()
                Toast.makeText(applicationContext, "Station 1:Old $old, New $new", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Not a Valid Socket", Toast.LENGTH_LONG).show()
            }

        }

        saveServer2.setOnClickListener{
            val ip = msg.text.toString()
            val checkIp = addressStore.isItAValidIpv4(ip)
            if(checkIp) {
                val old = addressStore.getAddress2()
                addressStore.saveAddress2(ip)
                val new = addressStore.getAddress2()
                Toast.makeText(applicationContext, "Station 2: Old $old, New $new", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Not a Valid Socket", Toast.LENGTH_LONG).show()
            }

        }
    }
    suspend fun sendTcpMessage(ipaddr:String) {
        val fullAddress = ipaddr.split(":")
        val client = Socket(fullAddress[0],fullAddress[1].toInt())
        val tcpReq = PrintWriter(client.getOutputStream(), true)
        val tcpRespond = BufferedReader(InputStreamReader(client.getInputStream()))
        tcpReq.println(coordinates)
        println(tcpRespond.readLine())
        client.close()
    }

    suspend fun sendUdpMessage(ipaddr: String) {
        val fullAddress = ipaddr.split(":")
        val address = fullAddress[0].toString()
        val ipAsInetAddressObject = InetAddress.getByName(address)
        val socketAddr = InetSocketAddress(ipAsInetAddressObject,fullAddress[1].toInt())
        val socket = DatagramSocket()
        socket.broadcast = true
        val sendData = coordinates.toByteArray()
        val packet = DatagramPacket(sendData,sendData.size,socketAddr)
        socket.send(packet)
    }

}