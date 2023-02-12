package dev.sparshg.bitslogin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first


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
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        val pref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE)
        val context = this
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

        val wifi = getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifi.isWifiEnabled) {
//                    Log.e("TAG", "onClick: not wifi " + it.toString())
            Toast.makeText(this, "Wi-Fi not connected", Toast.LENGTH_LONG).show()
            return
        }

        val stringRequest: StringRequest =
            object : StringRequest(Method.POST, address, Response.Listener {
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                pref.edit().putBoolean("enabled", false).apply()
                VolleySingleton.isEmpty = true
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
            }, Response.ErrorListener {
//                    Log.e("TAG", "Volley Error: $it")
                val e = when (it) {
                    is TimeoutError, is NoConnectionError -> " Timeout"
                    is AuthFailureError -> " AuthFailure"
                    is ServerError -> " Server"
                    is NetworkError -> " Network"
                    else -> ""
                }
                Toast.makeText(context, "Login$e Error", Toast.LENGTH_SHORT).show()
                pref.edit().putBoolean("enabled", false).apply()
                VolleySingleton.isEmpty = true
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
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
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        val networkRequest =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                        VolleySingleton.getInstance(context.applicationContext).addToRequestQueue(stringRequest)
                    }
                }
            })

        val isRunning = runBlocking { Store(context).service.first() }
        if (isRunning) {
            try {
                context.startForegroundService(Intent(context, LoginService::class.java))
            } catch (e: Exception) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    getString(R.string.notiferr2),
                    getString(R.string.notiferr2),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.description = getString(R.string.notiferr2)
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
                notificationManager.notify(
                    3,
                    Notification.Builder(this, getString(R.string.notiferr2))
                        .setContentTitle("Auto-Login Service was killed")
                        .setContentText("Re-open the app to run the service and disable battery optimizations. Device specific instructions are available in the app.")
                        .setSmallIcon(R.drawable.ic_next).build()
                )
            }
        }
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
        scope.launch {
            Store(applicationContext).setQsAdded(false)
        }
    }

}