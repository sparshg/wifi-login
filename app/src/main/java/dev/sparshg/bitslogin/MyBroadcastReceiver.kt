package dev.sparshg.bitslogin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.currentRecomposeScope
import androidx.core.content.ContextCompat.getSystemService
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(
                WifiManager.EXTRA_WIFI_STATE, -1
            ) == WifiManager.WIFI_STATE_ENABLED
        ) {
            val pref = context.getSharedPreferences(
                context.getString(R.string.pref_name), Context.MODE_PRIVATE
            )
            val username = pref.getString("username", null)
            val password = pref.getString("password", null)
            val address = when (runBlocking { Store(context).address.first() }) {
                1 -> "https://campnet.bits-goa.ac.in:8090/login.xml"
                2 -> "https://172.16.0.30:8090/login.xml"
                else -> "https://fw.bits-pilani.ac.in:8090/login.xml"
            }
            Log.e("TAG", address)
            if (username == null || password == null) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    context.getString(R.string.notiferr),
                    context.getString(R.string.notiferr),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.description = context.getString(R.string.notiferr)
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
                notificationManager.notify(
                    2,
                    Notification.Builder(context, context.getString(R.string.notiferr))
                        .setContentTitle("Wi-Fi Login Credentials not set")
                        .setContentText("Please set your username and password in the app")
                        .setSmallIcon(R.drawable.ic_next).build()
                )
//                Toast.makeText(this.applicationContext, "Wi-Fi credentials not found", Toast.LENGTH_LONG).show()
                return
            }

            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest =
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
            val editor = pref.edit()
            val stringRequest: StringRequest = object : StringRequest(Request.Method.POST,
                address,
                Response.Listener {
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    editor.putBoolean("enabled", false).apply()
                    TileService.requestListeningState(
                        context, ComponentName(context, MyQSTileService::class.java)
                    )
                    VolleySingleton.isEmpty = true
                },
                Response.ErrorListener {
//                    Log.e("TAG", "Volley Error: $it")
                    val e = when (it) {
                        is TimeoutError, is NoConnectionError -> " Timeout"
                        is AuthFailureError -> " AuthFailure"
                        is ServerError -> " Server"
                        is NetworkError -> " Network"
                        else -> ""
                    }
                    Toast.makeText(context, "Login$e Error", Toast.LENGTH_SHORT).show()
                    editor.putBoolean("enabled", false).apply()
                    TileService.requestListeningState(
                        context, ComponentName(context, MyQSTileService::class.java)
                    )
                    VolleySingleton.isEmpty = true
                }) {
                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["mode"] = "191"
                    params["username"] = username
                    params["password"] = password
                    params["a"] = System.currentTimeMillis().toString()
                    return params
                }

                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded; charset=UTF-8"
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        super.onAvailable(network)
                        connectivityManager.bindProcessToNetwork(network)

                        if (VolleySingleton.isEmpty) {
                            editor.putBoolean("enabled", true).apply()
                            VolleySingleton.getInstance(context.applicationContext).addToRequestQueue(stringRequest)
//                            TileService.requestListeningState(
//                                context, ComponentName(context, MyQSTileService::class.java)
//                            )
                        }
                    }

                })
        }
    }
}