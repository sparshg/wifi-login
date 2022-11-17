package dev.sparshg.bitslogin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.*


class MyQSTileService : TileService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onTileAdded() {
        super.onTileAdded()
//        Log.e("TAG", "onTileAdded: ")
        scope.launch {
            Store(applicationContext).setQsAdded(true)
        }
        getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", false).apply()
    }
    // Called when your app can update your tile.

    override fun onStartListening() {
        super.onStartListening()
//        Toast.makeText(this.applicationContext, "ON", Toast.LENGTH_SHORT).show()
//        Log.e(
//            "TAG", "onStartListening: " + this.getSharedPreferences(
//                this.getString(R.string.pref_name), Context.MODE_PRIVATE
//            ).getBoolean(
//                "enabled", false
//            )
//        )
//        qsTile.label = resources.getString(R.string.qs_label);
//    qsTile.contentDescription = state.label
        qsTile.state = if (getSharedPreferences(
                getString(R.string.pref_name), Context.MODE_PRIVATE
            ).getBoolean(
                "enabled", false
            )
        ) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
//        Log.e("TAG", "onStartListening: " + VolleySingleton.isEmpty)
//        qsTile.state = if (VolleySingleton.isEmpty) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE

//    qsTile.icon = state.icon
        qsTile.updateTile()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
//        Log.e("TAG", "onStopListening: ")
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        val pref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE)
//        val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val ssid = wifiManager.connectionInfo.ssid
//        Log.e("TAG", "doWork: $ssid")
//        pref.edit().putBoolean("enabled", false).apply()

        if (!pref.getBoolean(
                "enabled", false
            )
//            && ssid.equals("\"BITS-STUDENT\"") || ssid.equals("\"BITS-STAFF\"") || ssid.equals("<unknown ssid>")
        ) {
//            Log.e("TAG", "onClick: ")
            // if not connected to wifi
            val username = pref.getString("username", null)
            val password = pref.getString("password", null)
            if (username == null || password == null) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    getString(R.string.notiferr),
                    getString(R.string.notiferr),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.description = getString(R.string.notiferr)
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
                notificationManager.notify(
                    2,
                    Notification.Builder(this, getString(R.string.notiferr))
                        .setContentTitle("Wi-Fi Login Credentials not set")
                        .setContentText("Please set your username and password in the app")
                        .setSmallIcon(R.drawable.ic_next).build()
                )
//                Toast.makeText(this.applicationContext, "Wi-Fi credentials not found", Toast.LENGTH_LONG).show()
                return
            }
            val connectivityManager =
                this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork).let {
                if (it == null || !it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                    Log.e("TAG", "onClick: not wifi")
                    Toast.makeText(this, "Not connected to WiFi", Toast.LENGTH_LONG).show()
                    return
                }
            }
            val context = this
            val stringRequest: StringRequest = object : StringRequest(Request.Method.POST,
                "https://fw.bits-pilani.ac.in:8090/login.xml",
                Response.Listener {
//                    Log.e("TAG", "Volley Successd")
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    pref.edit().putBoolean("enabled", false).apply()
                    VolleySingleton.isEmpty = true
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                },
                Response.ErrorListener {
//                    Log.e("TAG", "Volley Error: $it")
                    Toast.makeText(context, "Login Timeout error", Toast.LENGTH_SHORT).show()
                    pref.edit().putBoolean("enabled", false).apply()
                    VolleySingleton.isEmpty = true
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }) {
                override fun getParams(): Map<String, String>? {
                    val params: MutableMap<String, String> = HashMap()
                    params["mode"] = "191"
                    params["username"] = username
                    params["password"] = password
                    params["a"] = System.currentTimeMillis().toString()
//                    Log.d("TAG", params.toString());
                    return params
                }

                override fun getBodyContentType(): String? {
                    return "application/x-www-form-urlencoded; charset=UTF-8"
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            val networkRequest =
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        super.onAvailable(network)
                        if (VolleySingleton.isEmpty) {
                            pref.edit().putBoolean("enabled", true).apply()
                            qsTile.state = Tile.STATE_ACTIVE
                            qsTile.updateTile()

                            connectivityManager.bindProcessToNetwork(network)
//                            Log.e("TAG", network.toString())
                            VolleySingleton.getInstance(context).addToRequestQueue(stringRequest)
                        }
                    }
                })
        }
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
//        Log.e("TAG", "onTileRemoved: ")
        scope.launch {
            Store(applicationContext).setQsAdded(false)
        }
    }

}