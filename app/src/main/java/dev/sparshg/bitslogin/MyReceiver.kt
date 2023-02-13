package dev.sparshg.bitslogin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toSet


class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = runBlocking { Store.getInstance(context).data.first() }
        context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", false).apply()
        if (settings.service) {
            context.startForegroundService(Intent(context, LoginService::class.java))
        }
    }
}