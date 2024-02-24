package dsv.un.gps

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.net.Socket

class sendToInternet : AppCompatActivity() {

    lateinit var msg : EditText
    lateinit var sendMsg: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_to_internet)

        msg = findViewById(R.id.ipaddr)
        sendMsg = findViewById(R.id.btnsendnet)

        sendMsg.setOnClickListener{
            lifecycleScope.launch {
                sendMessage()
            }
        }
    }
    suspend fun sendMessage() {
        val ipaddr = msg.text.toString()
        val fullAddress = ipaddr.split(":")
        print(0)
        val client = Socket(fullAddress[0],fullAddress[1].toInt())
        client.outputStream.write("Hola".toByteArray())
        client.close()

    }
}