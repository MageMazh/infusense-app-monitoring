package com.d121211069.infusense.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.d121211069.infusense.R
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.notification.AlarmService
import com.d121211069.infusense.notification.InfusNotificationManager
import com.d121211069.infusense.ui.main.MainActivity
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonitorService : Service() {

    // Param dari UI
    private var roomId: String = ""
    private var targetTPM: Int = 20
    private var minVol: Int = 100
    private var critVol: Int = 50

    // Firebase
    private var dbRef: DatabaseReference? = null
    private var valueListener: ValueEventListener? = null

    // DataStore & scope
    private lateinit var prefs: ThresholdPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Foreground content intent
    private var contentPI: PendingIntent? = null
    private var lastFgVol: Int? = null
    private var lastFgTpm: Int? = null

    // Mencegah alarm spam   
    private val TPMMutex = Mutex()
    private val noDripMutex = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createFgChannel()
        prefs = ThresholdPreferences.getInstance(applicationContext.thresholdDataStore)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITOR) {
            // Lepas listener, stop alarm, hentikan Foreground notif, dan stop service
            valueListener?.let { dbRef?.removeEventListener(it) }
            valueListener = null
            dbRef = null
            stopAlarm()
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(FG_ID)
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        // Start awal dari Activity saat background
        roomId    = intent?.getStringExtra(EXTRA_ROOM_ID).orEmpty()
        targetTPM = intent?.getIntExtra(EXTRA_TARGET_TPM, targetTPM) ?: targetTPM
        minVol    = intent?.getIntExtra(EXTRA_MIN_VOL, minVol) ?: minVol
        critVol   = intent?.getIntExtra(EXTRA_CRIT_VOL, critVol) ?: critVol
        if (roomId.isBlank()) return START_NOT_STICKY

        startAsForeground("Memantau $roomId")
        startListening(roomId)

        return START_STICKY
    }

    private fun createFgChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(FG_CHANNEL_ID, "Monitoring Infus", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun startAsForeground(text: String) {
        val intent = Intent(this, MainActivity::class.java).apply { putExtra("from_notification", true) }
        contentPI = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, FG_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_infus)
            .setContentTitle("Infusense berjalan di latar belakang")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentPI)
            .build()

        startForeground(FG_ID, notif)
    }

    // Tambahkan util untuk update isi FG notif (Volume & TPM)
    private fun updateForeground(volume: Int?, tpm: Int?) {
        // Hindari update jika tidak ada perubahan nilai
        if (lastFgVol == volume && lastFgTpm == tpm) return

        val volText = volume?.let { "$it ml" } ?: "-"
        val tpmText = tpm?.let { "$it TPM" } ?: "-"

        val line1 = "Kamar: $roomId"
        val line2 = "Volume: $volText • Tetes: $tpmText"

        val notif = NotificationCompat.Builder(this, FG_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_infus)
            .setContentTitle("Infusense berjalan di latar belakang")
            .setContentText(line2)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(line1)
                    .addLine(line2)
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentPI)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(FG_ID, notif)

        lastFgVol = volume
        lastFgTpm = tpm
    }

    private fun startListening(room: String) {
        valueListener?.let { dbRef?.removeEventListener(it) }
        dbRef = FirebaseDatabase.getInstance().getReference("patients").child(room)

        valueListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val status = s.child("status").getValue(String::class.java) ?: ""
                if (status == "initializing") {
                    stopAlarm()

                    // tampilkan FG notif dengan placeholder
                    updateForeground(volume = null, tpm = null)
                    return
                }

                val volume = s.child("volume").getValue(Int::class.java) ?: 0
                val tpmObj = s.child("drip_rate_tpm").getValue(Int::class.java)
                val tpm = tpmObj ?: -1

                handleVolume(volume)
                handleTPM(tpm)

                // update FG notif dengan nilai baru
                updateForeground(
                    volume = volume,
                    tpm = if (tpm >= 0) tpm else null
                )
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        }
        dbRef?.addValueEventListener(valueListener!!)
    }

    // Notifikasi minimum/kritis di background
    private fun handleVolume(volume: Int) {
        val now = System.currentTimeMillis()

        serviceScope.launch {
            val reachedMin = prefs.reachedMinFlow().first()
            val reachedCrit = prefs.reachedCritFlow().first()

            when {
                volume <= critVol -> {
                    if (!reachedCrit) {
                        val it = Intent(this@MonitorService, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_PLAY
                            putExtra(AlarmService.EXTRA_TITLE, "ALARM: Infus Kritis!")
                            putExtra(AlarmService.EXTRA_MESSAGE, "Volume infus tersisa $volume ml")
                        }
                        startService(it)
                        prefs.setReachedCrit(true)
                        prefs.setReachedMin(false)
                    }
                }
                volume <= minVol -> {
                    if (!reachedMin) {
                        InfusNotificationManager.showNotification(
                            this@MonitorService,
                            "Peringatan Infus",
                            "Volume mencapai batas minimum: $volume ml"
                        )
                        prefs.setReachedMin(true)
                        prefs.setLastMinNotifyAt(now)
                        prefs.setReachedCrit(false)
                    }
                }
                else -> {
                    // Normal → reset flags
                    if (reachedMin || reachedCrit) {
                        prefs.setReachedMin(false)
                        prefs.setReachedCrit(false)
                    }
                    stopAlarm()
                }
            }
        }
    }

    // Notifikasi cepat/lambat
    private fun handleTPM(tpm: Int) {
        if (tpm < 0) return

        if (tpm == 0) {
            serviceScope.launch {
                noDripMutex.withLock {
                    val now = System.currentTimeMillis()

                    val lastNoDripAlarm = prefs.lastNoDripAlarmAtFlow().first()
                    val reachedNoDrip   = prefs.reachedNoDripFlow().first()
                    val throttled       = (now - lastNoDripAlarm) < 65_000L

                    if (!throttled) {
                        prefs.setReachedNoDrip(false)
                    }

                    if (!reachedNoDrip) {
                        val it = Intent(this@MonitorService, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_PLAY
                            putExtra(AlarmService.EXTRA_TITLE, "ALARM: Tidak Ada Tetesan")
                            putExtra(AlarmService.EXTRA_MESSAGE, "Infus tidak menetes.")
                        }
                        startService(it)

                        prefs.setLastNoDripAlarmAt(now)
                        prefs.setReachedNoDrip(true)
                    }
                }
            }
            return
        }

        // Ada tetesan (tpm > 0) → reset flag
        serviceScope.launch {
            prefs.setReachedNoDrip(false)
            prefs.setLastNoDripAlarmAt(System.currentTimeMillis())
        }

        val fast = tpm > (targetTPM + 3)
        val slow = tpm < (targetTPM - 3)
        if (!fast && !slow) {
            // reset batasan speed agar bisa warning lagi nanti
            serviceScope.launch { prefs.setLastSpeedNotifyAt(0L) }
            return
        }

        serviceScope.launch {
            TPMMutex.withLock {
                val now = System.currentTimeMillis()
                val last = prefs.lastSpeedNotifyAtFlow().first()
                val throttled = (now - last) < 60_000L
                if (!throttled) {
                    val pesan = if (fast)
                        "Tetesan infus terlalu cepat (TPM = $tpm). Periksa klep/ketinggian botol."
                    else
                        "Tetesan infus terlalu lambat (TPM = $tpm). Pastikan tidak ada sumbatan atau penurunan tekanan."

                    InfusNotificationManager.showNotification(
                        this@MonitorService,
                        "Tetesan Kurang Stabil",
                        pesan
                    )

                    prefs.setLastSpeedNotifyAt(now)
                }
            }
        }
    }

    private fun stopAlarm() {
        val stop = Intent(this, AlarmService::class.java).apply { action =
            AlarmService.ACTION_STOP
        }
        startService(stop)
    }

    override fun onDestroy() {
        valueListener?.let { dbRef?.removeEventListener(it) }
        valueListener = null
        dbRef = null
        stopAlarm()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Kalau user swipe task, matikan service agar hemat
        valueListener?.let { dbRef?.removeEventListener(it) }
        valueListener = null
        dbRef = null
        stopAlarm()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(FG_ID)
        stopForeground(true)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val EXTRA_ROOM_ID    = "extra_room_id"
        const val EXTRA_TARGET_TPM = "extra_target_tpm"
        const val EXTRA_MIN_VOL    = "extra_min_vol"
        const val EXTRA_CRIT_VOL   = "extra_crit_vol"

        const val ACTION_STOP_MONITOR = "action_stop_monitor"

        private const val FG_CHANNEL_ID = "infus_monitor_channel"
        private const val FG_ID = 2001
    }
}
