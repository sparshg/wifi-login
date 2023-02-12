package dev.sparshg.bitslogin

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.content.ContextCompat.registerReceiver


class LoginService : Service() {

    private lateinit var receiver: MyBroadcastReceiver
    companion object {
        var IS_RUNNING = false
    }
    override fun onCreate() {
        super.onCreate()
        receiver = MyBroadcastReceiver()
        IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION).also { intentFilter ->
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        IS_RUNNING = true
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
        IS_RUNNING = false
        unregisterReceiver(receiver)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

}