package com.d121211069.infusense.ui.alerts

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.d121211069.infusense.R
import com.d121211069.infusense.databinding.FragmentAlertsBinding
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.ui.edittreshold.EditThresholdActivity
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {
    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding as FragmentAlertsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val thresholdPrefs = ThresholdPreferences.getInstance(requireContext().thresholdDataStore)

        viewLifecycleOwner.lifecycleScope.launch {
            thresholdPrefs.getThreshold().collect { threshold ->
                val min = threshold.min
                val max = threshold.max
                val drip = threshold.drip
                val userName = threshold.user
                val hospitalName = threshold.hospital

                binding.descDrop.text = SpannableStringBuilder().apply {
                    append("Stabil ~")
                    val start = length
                    append("$drip")
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" TPM. Alarm jika tidak stabil")
                }

                binding.descNotif.text = SpannableStringBuilder().apply {
                    append("Notifikasi jika volume cairan mencapai ≤ ")
                    val start = length
                    append("$min")
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ml")
                }

                binding.descAlarm.text = SpannableStringBuilder().apply {
                    append("Alarm jika volume cairan hampir habis! Tersisa ≤ ")
                    val start = length
                    append("$max")
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ml")
                }

                binding.descPatientInfo.text =
                    getString(R.string.desc_information_pasien, userName, hospitalName)
            }
        }

        binding.buttonEditThreshold.setOnClickListener {
            val intent = Intent(activity, EditThresholdActivity::class.java)
            startActivity(intent)
        }
    }
}
