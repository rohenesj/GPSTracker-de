package dsv.un.gps

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_to_internet)

        msg = findViewById(R.id.ipaddr)
        sendMsg = findViewById(R.id.btnsendnet)
        sendUdp = findViewById(R.id.btnudp)
        sendBoth = findViewById(R.id.bothprotocols)

        sendMsg.setOnClickListener{
            lifecycleScope.launch(Dispatchers.IO) {
                //sendTcpMessage()
                sendTcpMessage()
            }
        }

        sendUdp.setOnClickListener{
            lifecycleScope.launch(Dispatchers.IO) {
                sendUdpMessage()
            }
        }

        sendBoth.setOnClickListener{
            lifecycleScope.launch(Dispatchers.IO) {
                sendTcpMessage()
                sendUdpMessage()
            }
        }

    }
    suspend fun sendTcpMessage() {
        val ipaddr = msg.text.toString()
        val fullAddress = ipaddr.split(":")
        print(0)
        val client = Socket(fullAddress[0],fullAddress[1].toInt())
        //client.outputStream.write("Hola".toByteArray())
        val tcpReq = PrintWriter(client.getOutputStream(), true)
        val tcpRespond = BufferedReader(InputStreamReader(client.getInputStream()))
        tcpReq.println("Prueba")
        println(tcpRespond.readLine())
        client.close()

    }

    suspend fun sendUdpMessage() {
        val ipaddr = msg.text.toString()
        val fullAddress = ipaddr.split(":")
        val address = fullAddress[0].toString()
        val ipAsInetAddressObject = InetAddress.getByName(address)
        val socketAddr = InetSocketAddress(ipAsInetAddressObject,fullAddress[1].toInt())
        println("1")
        val socket = DatagramSocket()
        socket.broadcast = true
        val testMsg = "UdpTest"
        val sendData = testMsg.toByteArray()
        //println("1")
        val packet = DatagramPacket(sendData,sendData.size,socketAddr)
        //println("2")
        socket.send(packet)
    }
}