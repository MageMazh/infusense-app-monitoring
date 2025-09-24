package com.d121211069.infusense.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.d121211069.infusense.R
import com.d121211069.infusense.databinding.ActivityMainBinding
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.service.MonitorService
import com.d121211069.infusense.ui.edittreshold.EditThresholdActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var thresholdPrefs: ThresholdPreferences

    private var latestMin: Int = 0
    private var latestCrit: Int = 0
    private var latestDrip: Int = 0
    private var latestRoomId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        thresholdPrefs = ThresholdPreferences.getInstance(applicationContext.thresholdDataStore)

        lifecycleScope.launch {
            val threshold = thresholdPrefs.getThreshold().first()

            val isEmpty = threshold.min == 0 && threshold.max == 0 && threshold.drip == 0
            if (isEmpty) {
                val intent = Intent(this@MainActivity, EditThresholdActivity::class.java)
                intent.putExtra("IS_FIRST_TIME", true)
                startActivity(intent)
                finish()
                return@launch
            }

            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            binding.navView.setupWithNavController(navController)
        }

        // observe threshold & simpan ke cache
        lifecycleScope.launch {
            thresholdPrefs.getThreshold()
                .distinctUntilChanged()
                .collect { t ->
                    latestMin = t.min
                    latestCrit = t.max
                    latestDrip = t.drip
                }
        }

        lifecycleScope.launch {
            latestRoomId = thresholdPrefs.getRoomId()
        }
    }

    override fun onResume() {
        super.onResume()
        // App aktif → pastikan service TIDAK jalan (hemat total)
        stopService(Intent(this, MonitorService::class.java))
    }

    override fun onStop() {
        super.onStop()
        // App ke background → nyalakan Foreground service
        if (latestRoomId.isNotBlank()) {
            val i = Intent(this, MonitorService::class.java).apply {
                putExtra(MonitorService.EXTRA_ROOM_ID, latestRoomId)
                putExtra(MonitorService.EXTRA_TARGET_TPM, latestDrip)
                putExtra(MonitorService.EXTRA_MIN_VOL, latestMin)
                putExtra(MonitorService.EXTRA_CRIT_VOL, latestCrit)
            }
            ContextCompat.startForegroundService(this, i)
        }
    }
}
