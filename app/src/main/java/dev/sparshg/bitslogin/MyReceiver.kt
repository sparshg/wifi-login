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
import androidx.core.content.ContextCompat.getSystemService
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first


class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
//        Log.e("MyReceiver", "onReceive: isRunning")
        val isRunning = runBlocking { Store(context).service.first() }
//        Log.e("MyReceiver", "onReceive: $isRunning")
        if (isRunning) {
            context.startService(Intent(context, MyForegroundService::class.java))
        }
    }
}