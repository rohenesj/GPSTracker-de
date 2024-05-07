package dsv.un.gps

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns

class Addresses(val context:Context) {



    val store: SharedPreferences = context.getSharedPreferences("Bases",0)
    
    fun saveAddress1(address:String){
        store.edit().putString("Address 1",address).apply()
    }

    fun saveAddress2(address:String){
        store.edit().putString("Address 2",address).apply()
    }
    fun carSelector(car:String){
        store.edit().putString("Car",car).apply()
    }
    fun getCar() :String {
        return store.getString("Car","")!!
    }
    fun getAddress1() :String {
        return store.getString("Address 1","")!!
    }

    fun getAddress2() :String {
        return store.getString("Address 2","")!!
    }

    fun isItAValidIpv4(ip:String): Boolean {
        val fullAddress = ip.split(":")
        val ipValid:Boolean = Patterns.IP_ADDRESS.matcher(fullAddress[0]).matches()
        return ipValid && fullAddress[1].toIntOrNull() != null
    }
}