package dev.sparshg.bitslogin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first


class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isRunning = runBlocking { Store(context).service.first() }
        context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", false).apply()
        if (isRunning) {
            context.startForegroundService(Intent(context, LoginService::class.java))
        }
    }
}