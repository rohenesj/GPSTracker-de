package dsv.un.gps

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import dsv.un.gps.RunService.Companion.addressStore
import android.os.Bundle
import android.widget.Button

class CarSelector : AppCompatActivity() {

    lateinit var select1 : Button
    lateinit var select2 : Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_selector)

        select1 = findViewById(R.id.selectCar1)
        select2 = findViewById(R.id.selectCar2)
        select1.setOnClickListener{
            addressStore.carSelector("1")
            finish()
        }
        select2.setOnClickListener{
            addressStore.carSelector("2")
            finish()
        }

    }
}