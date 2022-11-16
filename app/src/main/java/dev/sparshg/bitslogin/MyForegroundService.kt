package dev.sparshg.bitslogin

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest


class MyForegroundService : Service() {
    private lateinit var receiver: MyBroadcastReceiver
    override fun onCreate() {
        super.onCreate()
        receiver = MyBroadcastReceiver()
        IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION).also { intentFilter ->
            registerReceiver(receiver, intentFilter)
        }
//        Log.e("TAG", "onCreate: ")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        Log.e("TAG", "onStartCommand: ")
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification: Notification = Notification.Builder(this, getString(R.string.notif))
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_next)
            .setContentIntent(pendingIntent)
//            .setTicker(getText(R.string.ticker_text))
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            getString(R.string.notif),
            getString(R.string.notif), NotificationManager.IMPORTANCE_LOW
        )
        notificationChannel.description = getString(R.string.notif)
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)

// Notification ID cannot be 0.
        startForeground(1, notification)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
//        Log.e("TAG", "onDestroy: ")
        unregisterReceiver(receiver)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

}