package dev.sparshg.bitslogin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // At boot and after an app update (which stops the running service).
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val settings = runBlocking { Store.getInstance(context).data.first() }
        if (settings.service) {
            context.startForegroundService(Intent(context, LoginService::class.java))
        }
    }
}