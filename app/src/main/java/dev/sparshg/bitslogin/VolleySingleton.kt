package dev.sparshg.bitslogin

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.android.volley.RequestQueue
import com.android.volley.Request
import com.android.volley.toolbox.Volley

class VolleySingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null
        var isEmpty = true
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also {
                    INSTANCE = it
                }
            }
    }
    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        isEmpty = false
        requestQueue.add(req)
    }
    fun cancelAll() {
        isEmpty = true
        requestQueue.cancelAll { true }
    }
}
