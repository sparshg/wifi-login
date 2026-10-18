package dev.sparshg.bitslogin

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the phone logged in to the campus Wi-Fi portal. Checks when Wi-Fi connects, when
 * Android flags a captive portal, when the screen turns on, and on an alarm: every minute
 * while the screen is on, every 15 minutes while it's off (plain timers stop in deep sleep).
 * Started from the app, at boot, by the Quick Settings tile and by its own alarm (ACTION_CHECK).
 */
class LoginService : Service() {

    companion object {
        const val ACTION_CHECK = "dev.sparshg.bitslogin.CHECK"
        private const val ONLINE_EVERY = 60_000L
        private const val RETRY_EVERY = 10_000L
        private const val ASLEEP_EVERY = 900_000L // screen off: wake the CPU at most every 15 min
        private const val KEEPALIVE_EVERY = 14_400_000L // 4 h, like the portal's own keepalive page
        private const val MAX_FAILS = 2 // FortiGate locks an account after 3 failures by default
        private const val HOLD = 1_800_000L
        // Goa and Hyderabad run Sophos portals. Hyderabad's only takes the login over plain HTTP to
        // an IP (like its own web page), so we only ever send it when this network redirects there.
        private const val GOA = "https://campnet.bits-goa.ac.in:8090"
        private const val HYDERABAD = "http://172.16.0.30:8090"
        private val SOPHOS = mapOf(1 to GOA, 2 to HYDERABAD)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private lateinit var cm: ConnectivityManager
    private lateinit var alarms: AlarmManager
    private lateinit var wakeLock: PowerManager.WakeLock
    @Volatile private var wifi: Network? = null
    @Volatile private var captive = false
    // Kept in prefs, so tile one-shots and restarts can't reset the lockout guard.
    private val prefs by lazy { getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE) }
    private var fails: Int
        get() = prefs.getInt("fails", 0)
        set(v) { if (v != fails) prefs.edit().putInt("fails", v).apply() }
    private var holdUntil: Long // wall clock, so a reboot doesn't stretch or skip it
        get() = prefs.getLong("holdUntil", 0)
        set(v) = prefs.edit().putLong("holdUntil", v).apply()
    private var noNetwork = 0
    private var keepaliveUrl: String? = null
    private var keepaliveAt = 0L

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wifi = network
            wake.trySend(Unit)
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            wifi = network
            val nowCaptive = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            if (nowCaptive && !captive) wake.trySend(Unit) // Android noticed the portal too
            captive = nowCaptive
        }

        override fun onLost(network: Network) {
            if (wifi == network) wifi = null
        }
    }

    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wake.trySend(Unit)
        }
    }

    override fun onCreate() {
        super.onCreate()
        cm = getSystemService(ConnectivityManager::class.java)
        alarms = getSystemService(AlarmManager::class.java)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bitslogin:check")
            .apply { setReferenceCounted(false) }
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), wifiCallback
        )
        ContextCompat.registerReceiver(
            this, screenOn, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        scope.launch { loop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationChannel = NotificationChannel(
            getString(R.string.notif), getString(R.string.notif), NotificationManager.IMPORTANCE_LOW
        )
        notificationChannel.description = getString(R.string.notif)
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = Notification.Builder(this, getString(R.string.notif))
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_next)
            .setContentIntent(pendingIntent)
            .build()
        // Notification ID cannot be 0.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: IllegalStateException) { // Android 12+ can refuse a background start (e.g. from the alarm)
            Log.w("LoginService", "not allowed to start in the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_CHECK) {
            wakeLock.acquire(180_000) // held from the alarm until the check is done
            wake.trySend(Unit)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        alarms.cancel(alarm())
        cm.unregisterNetworkCallback(wifiCallback)
        unregisterReceiver(screenOn)
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun alarm() = PendingIntent.getForegroundService(
        this, 1, Intent(this, LoginService::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private suspend fun loop() {
        var timer = Long.MAX_VALUE // screen on: a plain timer (the CPU stays awake); off: only the alarm
        while (true) {
            withTimeoutOrNull(timer) { wake.receive() }
            wakeLock.acquire(180_000) // worst case: probe, form, POST, 5 confirmation probes
            try {
                // A tile one-shot can get here before onAvailable has reported the Wi-Fi.
                @Suppress("DEPRECATION")
                val network = wifi ?: cm.allNetworks.firstOrNull { n ->
                    cm.getNetworkCapabilities(n)?.run {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    } == true
                }
                val wait = try {
                    network?.let { check(it) } ?: ASLEEP_EVERY
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) { // never let one bad check end the loop for good
                    Log.w("LoginService", "check failed", e)
                    RETRY_EVERY
                }
                // Switched off in the app: this start was only the tile asking for one check.
                if (!Store.getInstance(this).data.first().service) {
                    stopSelf()
                    return
                }
                val asleep = !getSystemService(PowerManager::class.java).isInteractive
                timer = if (asleep) Long.MAX_VALUE else wait
                // The alarm is the backup, and the only clock once the screen is off (timers stop in
                // deep sleep). Every 15 min stays within Android's alarm quotas without the exemption.
                alarms.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + maxOf(wait, ASLEEP_EVERY),
                    alarm()
                )
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    /** One check; returns how long to wait before the next. */
    private suspend fun check(network: Network): Long {
        val settings = Store.getInstance(this).data.first()
        val state = Portal.probe(network)
        if (state != Portal.NoNetwork) noNetwork = 0
        return when (state) {
            Portal.Online -> {
                fails = 0
                if (settings.keepalive) keepalive(network) // switchable in the app: it costs ~6 requests a day
                ONLINE_EVERY
            }

            is Portal.FortiGate -> { // Pilani
                if (settings.address != 0) return ONLINE_EVERY // a visitor's Goa/Hyderabad login isn't for this firewall
                keepaliveUrl = null // that session is over
                login(network, settings) { u, p -> Portal.loginFortiGate(network, state.url, u, p) }
                RETRY_EVERY
            }

            is Portal.Sophos -> { // Goa or Hyderabad: only the campus picked in the app
                if (state.origin != SOPHOS[settings.address]) return ONLINE_EVERY
                login(network, settings) { u, p -> Portal.loginSophos(network, "${state.origin}/login.xml", u, p) }
                RETRY_EVERY
            }

            Portal.OtherPortal, Portal.NoNetwork -> {
                if (state == Portal.NoNetwork && ++noNetwork == 3) explainNoNetwork(network)
                if (settings.address != 1) return if (state == Portal.NoNetwork) RETRY_EVERY else ONLINE_EVERY
                // Goa may block instead of redirecting, so try its login anyway: it's HTTPS to a named
                // host, and off campus that name doesn't resolve, which counts as nothing sent.
                login(network, settings) { u, p -> Portal.loginSophos(network, "$GOA/login.xml", u, p) }
                RETRY_EVERY
            }
        }
    }

    private suspend fun login(network: Network, s: Settings, attempt: (String, String) -> Portal.Outcome) {
        if (s.username.isEmpty() || s.password.isEmpty()) {
            notify("Wi-Fi Login Credentials not set", "Please set your username and password in the app")
            return
        }
        // Turned down twice before with these exact credentials: wait until they're saved again.
        val creds = (s.username + "\n" + s.password).hashCode()
        if (prefs.contains("rejected") && prefs.getInt("rejected", 0) == creds) return
        if (fails >= MAX_FAILS && System.currentTimeMillis() < holdUntil) return

        val outcome = attempt(s.username, s.password)
        if (outcome == Portal.NoForm) return
        val accepted = (outcome as? Portal.Answered)?.accepted == true // a keepalive token, or Sophos said LIVE
        repeat(5) {
            delay(2_000)
            if (accepted || Portal.probe(network) == Portal.Online) {
                fails = 0
                keepaliveUrl = (outcome as? Portal.Answered)?.keepalive
                keepaliveAt = SystemClock.elapsedRealtime()
                // Android re-checks right away: clears "Sign in to network" and moves traffic back to Wi-Fi.
                cm.reportNetworkConnectivity(network, true)
                getSystemService(NotificationManager::class.java).cancel(2)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginService, "Login successful", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        if (++fails < MAX_FAILS) return
        if (outcome is Portal.Answered) { // the portal replied twice and we're still out: wrong password
            prefs.edit().putInt("rejected", creds).apply()
            notify("Wi-Fi login rejected", "Check your username and password in the app")
        } else {
            holdUntil = System.currentTimeMillis() + HOLD
            notify("Wi-Fi login failing", "Trying again in 30 minutes")
        }
    }

    /** Wi-Fi is up but nothing gets through: a VPN or strict Private DNS can block the login. */
    private fun explainNoNetwork(network: Network) {
        val vpn = cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val strictDns = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            cm.getLinkProperties(network)?.privateDnsServerName != null
        if (vpn) notify("A VPN may be blocking Wi-Fi login", "Exclude this app from the VPN, or pause it while logging in")
        else if (strictDns) notify("Private DNS is blocking Wi-Fi login", "Set Private DNS to Automatic in Settings")
    }

    private fun keepalive(network: Network) {
        val url = keepaliveUrl ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - keepaliveAt < KEEPALIVE_EVERY) return
        keepaliveAt = now
        Portal.keepalive(network, url)
    }

    private fun notify(title: String, text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(getString(R.string.notiferr), getString(R.string.notiferr), NotificationManager.IMPORTANCE_DEFAULT)
        )
        notificationManager.notify(
            2,
            Notification.Builder(this, getString(R.string.notiferr))
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_next)
                .setOnlyAlertOnce(true) // repeated checks update it quietly
                .build()
        )
    }
}
