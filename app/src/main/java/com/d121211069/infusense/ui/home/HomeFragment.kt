package com.d121211069.infusense.ui.home

import android.content.Intent
import android.graphics.Outline
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.d121211069.infusense.R
import com.d121211069.infusense.data.local.entity.HistoryEntity
import com.d121211069.infusense.databinding.FragmentHomeBinding
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.factory.ViewModelFactory
import com.d121211069.infusense.notification.AlarmService
import com.d121211069.infusense.notification.InfusNotificationManager
import com.d121211069.infusense.util.InfusStatus
import com.d121211069.infusense.util.UiState
import com.d121211069.infusense.util.currentDateTimeFormatted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels { ViewModelFactory.getInstance(requireContext()) }

    private lateinit var prefs: ThresholdPreferences
    private var user: String = "-"
    private var hospital: String = "-"

    private val maxVolume = 500f

    private var lastNotifTime: Long = 0
    private var lastNotifTimeNoDrip: Long = 0
    private val notifyDelayMs = 68_000L

    private var offsetListener: com.google.firebase.database.ValueEventListener? = null
    private lateinit var offsetRef: DatabaseReference


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = ThresholdPreferences.getInstance(requireContext().thresholdDataStore)

        // Ambil threshold
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.thresholdFlow.collect { t ->
                viewModel.currentMin = t.min
                viewModel.currentMax = t.max
                viewModel.targetTPM  = t.drip
                user = t.user
                hospital = t.hospital
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val roomId = prefs.getRoomId()
            if (roomId.isBlank()) {
                updateUIState(UiState.INPUT_ROOM)
                setupRoomInput(prefs)
            } else {
                updateUIState(UiState.ACTIVE_ROOM, roomId)
                showDataUI(roomId)
            }
        }

        binding.progressContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View?, outline: Outline?) {
                v?.let { outline?.setRoundRect(0, 0, it.width, it.height, 36f * it.resources.displayMetrics.density) }
            }
        }
        binding.progressContainer.clipToOutline = true

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                MaterialAlertDialogBuilder(requireContext())
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari kamar bed ini?")
                .setPositiveButton("Ya") { dialog, _ ->
                    lifecycleScope.launch {
                        requireContext().startService(
                            Intent(requireContext(), AlarmService::class.java).apply { action = AlarmService.ACTION_STOP }
                        )
                        requireContext().startService(
                            Intent(requireContext(), com.d121211069.infusense.service.MonitorService::class.java).apply {
                                action = com.d121211069.infusense.service.MonitorService.ACTION_STOP_MONITOR
                            }
                        )

                        viewModel.stopObserving()

                        prefs.clearRoomId()
                        viewModel.resetStatusFlags()
                        updateUIState(UiState.INPUT_ROOM)
                        setupRoomInput(prefs)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            }
        }
    }

    private fun setupRoomInput(prefs: ThresholdPreferences) {
        binding.btnSaveRoom.setOnClickListener {
            val room = binding.etRoomInput.text.toString().trim()
            val pattern = Regex("^room-\\d{3}-bed-\\d{2}$")
            if (!room.matches(pattern)) {
                Toast.makeText(requireContext(), "Format kamar tidak sesuai. Contoh: room-001-bed-01", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dbRef = FirebaseDatabase.getInstance().getReference("patients").child(room)
            dbRef.get().addOnSuccessListener { snap ->
                if (snap.exists()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        prefs.saveRoomId(room)
                        updateUIState(UiState.ACTIVE_ROOM, room)
                        showDataUI(room)
                    }
                } else {
                    Toast.makeText(requireContext(), "Kamar tidak ditemukan, coba perhatikan kembali nomor room dan bed", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal terhubung ke Firebase: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUIState(state: UiState, roomId: String = "") {
        when (state) {
            UiState.INPUT_ROOM -> {
                binding.roomInputContainer.visibility = View.VISIBLE
                binding.etRoomInput.visibility = View.VISIBLE
                binding.btnSaveRoom.visibility = View.VISIBLE
                binding.cardCurrentVolume.visibility = View.GONE
                binding.cardCurrentDropRate.visibility = View.GONE
                binding.btnLogout.visibility = View.GONE
                binding.tvRoomPrompt.text = "Halo! Masukkan kode kamar pasien:"
            }
            UiState.ACTIVE_ROOM -> {
                binding.etRoomInput.visibility = View.GONE
                binding.btnSaveRoom.visibility = View.GONE
                binding.cardCurrentVolume.visibility = View.VISIBLE
                binding.cardCurrentDropRate.visibility = View.VISIBLE
                binding.btnLogout.visibility = View.VISIBLE

                val fullText = "Halo!\nKamar Aktif: $roomId"
                val spannable = SpannableString(fullText)
                val startIndex = fullText.indexOf("Kamar Aktif: $roomId")
                if (startIndex != -1) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                binding.tvRoomPrompt.text = spannable
                binding.tvRoomPrompt.visibility = View.VISIBLE
            }
        }
    }

    private fun observeViewModel() {
        viewModel.status.observe(viewLifecycleOwner) { status ->
            viewModel.currentStatus = status
            if (status == "initializing") {
                binding.tvDropRateStatus.text = "Status:\nInfus baru dipasang\nTPM akan muncul sebentar lagi"

                // pastikan alarm mati saat initializing
                viewLifecycleOwner.lifecycleScope.launch {
                    val prefs = ThresholdPreferences.getInstance(requireContext().thresholdDataStore)
                    prefs.setReachedMin(false)
                    prefs.setReachedCrit(false)
                }
                requireContext().startService(
                    Intent(requireContext(), AlarmService::class.java).apply {
                        action = AlarmService.ACTION_STOP
                    }
                )
            }
        }

        viewModel.volume.observe(viewLifecycleOwner) { volumeD ->
            val volume = volumeD.toInt()
            binding.tvVolumeText.text = "$volume ml"
            if (viewModel.currentStatus == "initializing") {
                binding.tvStatus.text = "Status: initializing"
            }
            updateVolumeFill(volume, viewModel.currentMin, viewModel.currentMax) // max dipakai sbg batas kritis
            handleVolumeStatusUI(volume)
        }

        viewModel.tpm.observe(viewLifecycleOwner) { tpm ->
            if (viewModel.currentStatus != "initializing") {
                binding.tvDropRateValue.text = "$tpm TPM"
                updateTPMStatusUI(tpm, viewModel.targetTPM)
            }
        }

        viewModel.interval.observe(viewLifecycleOwner) { interval ->
            binding.tvIntervalValue.text = "Interval: %.1f detik".format(interval)
        }
    }

    private fun handleVolumeStatusUI(volume: Int) {
        val min = viewModel.currentMin
        val crit = viewModel.currentMax

        viewLifecycleOwner.lifecycleScope.launch {
            val reachedMin = prefs.reachedMinFlow().first()
            val reachedCrit = prefs.reachedCritFlow().first()

            when {
                volume <= crit -> {
                    if (!reachedCrit) {
                        val intent = Intent(requireContext(), AlarmService::class.java).apply {
                            action = AlarmService.ACTION_PLAY
                            putExtra(AlarmService.EXTRA_TITLE, "ALARM: Infus Kritis!")
                            putExtra(AlarmService.EXTRA_MESSAGE, "Volume infus tersisa $volume ml")
                        }
                        requireContext().startService(intent)
                        
                        prefs.setReachedCrit(true)
                        prefs.setReachedMin(false)
                    }
                }
                volume <= min -> {
                    if (!reachedMin) {
                        prefs.setReachedMin(true)
                        prefs.setReachedCrit(false)

                        InfusNotificationManager.showNotification(
                            context = requireContext(),
                            title = "Peringatan Infus",
                            message = "Volume infus mencapai batas minimum: $volume ml"
                        )

                        viewModel.insert(
                            HistoryEntity(
                                user = user,
                                dateTime = currentDateTimeFormatted(),
                                location = hospital,
                                description = "Volume mencapai batas minimal",
                                status = InfusStatus.MINIMAL
                            )
                        )
                    }
                }
                else -> {
                    if (reachedMin || reachedCrit) {
                        prefs.setReachedMin(false)
                        prefs.setReachedCrit(false)
                    }
                }
            }
        }
    }

    private fun styled(full: String, colorRes: Int): SpannableString {
        val start = full.indexOf(":") + 2
        val end = full.length
        val s = SpannableString(full)
        s.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(requireContext().getColor(colorRes)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    private fun updateTPMStatusUI(tpm: Int, targetTPM: Int) {
        val (statusText, colorRes) = when {
            tpm == 0 -> "Tidak Ada Tetesan" to R.color.dark_gray
            tpm > targetTPM + 3 -> "Terlalu Cepat" to R.color.red
            tpm < targetTPM - 3 -> "Terlalu Lambat" to R.color.orange
            else -> "Baik" to R.color.blue_dark
        }
        val fullText = "Status: $statusText"
        binding.tvDropRateStatus.text = styled(fullText, colorRes)

        val now = System.currentTimeMillis()

        when (statusText) {
            "Terlalu Cepat", "Terlalu Lambat" -> {
                if (now - lastNotifTime > notifyDelayMs) {
                    val historyItem = HistoryEntity(
                        user = user,
                        dateTime = currentDateTimeFormatted(),
                        location = hospital,
                        description = "Laju tetes terdeteksi tidak stabil",
                        status = InfusStatus.DRIP_ABNORMAL
                    )
                    lifecycleScope.launch {
                        viewModel.insert(historyItem)
                    }

                    val title = "Tetesan Kurang Stabil"

                    val pesan = when (statusText) {
                        "Terlalu Cepat" -> "Tetesan infus terlalu cepat (TPM = $tpm). Mohon periksa klep atau ketinggian botol infus."
                        "Terlalu Lambat" -> "Tetesan infus terlalu lambat (TPM = $tpm). Pastikan tidak ada sumbatan atau penurunan tekanan."
                        else -> "Perhatikan tetesan infus"
                    }

                    InfusNotificationManager.showNotification(requireContext(), title, pesan)

                    lastNotifTime = now
                }
            }
            "Tidak Ada Tetesan" -> {
                if (now - lastNotifTimeNoDrip > notifyDelayMs) {
                    val historyItem = HistoryEntity(
                        user = user,
                        dateTime = currentDateTimeFormatted(),
                        location = hospital,
                        description = "Laju tetes terdeteksi tidak ada menetes",
                        status = InfusStatus.DRIP_STOP
                    )
                    lifecycleScope.launch {
                        viewModel.insert(historyItem)
                    }

                    val intent = Intent(requireContext(), AlarmService::class.java).apply {
                        action = AlarmService.ACTION_PLAY
                        putExtra(AlarmService.EXTRA_TITLE, "ALARM: Tidak Ada Tetesan")
                        putExtra(AlarmService.EXTRA_MESSAGE, "Infus tidak menetes selama 1 menit terakhir")
                    }
                    requireContext().startService(intent)

                    lastNotifTimeNoDrip = now
                }
            }
            else -> {
                lastNotifTime = 0
                lastNotifTimeNoDrip = 0
            }
        }
    }


    private fun showDataUI(roomId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeData(roomId)
            observeViewModel()
        }
    }

    private fun updateVolumeFill(volume: Int, min: Int, crit: Int) {
        val fillRatio = (volume / maxVolume).coerceIn(0f, 1f)
        val containerHeight = binding.progressContainer.height
        if (containerHeight == 0) {
            binding.progressContainer.post { updateVolumeFill(volume, min, crit) }
            return
        }
        val fillHeight = (containerHeight * fillRatio).toInt()
        val lp = binding.volumeFill.layoutParams
        lp.height = fillHeight
        binding.volumeFill.layoutParams = lp

        if (viewModel.currentStatus != "initializing") {
            val fullText = when {
                volume > min -> "Status: Baik"
                volume in (crit + 1)..min -> "Status: Peringatan"
                volume in 1..crit -> "Status: Kritis"
                else -> "Status: Kosong"
            }
            val colorRes = when {
                volume > min -> R.color.blue_dark
                volume in (crit + 1)..min -> R.color.orange
                volume in 1..crit -> R.color.red
                else -> R.color.dark_gray
            }
            binding.tvStatus.text = styled(fullText, colorRes)
        }
    }

    override fun onDestroyView() {
        offsetListener?.let { offsetRef.removeEventListener(it) }
        offsetListener = null
        _binding = null
        super.onDestroyView()
    }
}
