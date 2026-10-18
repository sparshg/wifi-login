package dev.sparshg.bitslogin

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Quick Settings tile: asks LoginService to check (and log in) right now. */
class MyQSTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileAdded() {
        super.onTileAdded()
        scope.launch { Store.getInstance(applicationContext).updateData { it.copy(qsAdded = true) } }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        scope.launch { Store.getInstance(applicationContext).updateData { it.copy(qsAdded = false) } }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        try {
            startForegroundService(Intent(this, LoginService::class.java).setAction(LoginService.ACTION_CHECK))
            Toast.makeText(this, "Checking Wi-Fi login…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { // a tile tap alone may not start a service (Android 14+): open the app
            val app = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, app, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(app)
            }
        }
    }
}
