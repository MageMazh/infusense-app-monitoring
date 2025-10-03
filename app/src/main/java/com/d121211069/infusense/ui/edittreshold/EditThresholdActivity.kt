package com.d121211069.infusense.ui.edittreshold

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.d121211069.infusense.R
import com.d121211069.infusense.databinding.ActivityEdittresholdBinding
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.ui.main.MainActivity
import kotlinx.coroutines.launch

class EditThresholdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdittresholdBinding

    private var isEditTextUpdating = false
    private var isSeekBarUpdating = false
    private lateinit var thresholdPrefs: ThresholdPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEdittresholdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        thresholdPrefs = ThresholdPreferences.getInstance(applicationContext.thresholdDataStore)

        val isFirstTime = intent.getBooleanExtra("IS_FIRST_TIME", false)
        showFirstTimeGreeting(isFirstTime)

        if (isFirstTime) {
            binding.tvTitle.text = getString(R.string.add_batasan)
            binding.btnBack.visibility = View.GONE
        } else {
            binding.tvTitle.text = getString(R.string.edit_batasan)
            binding.btnBack.setOnClickListener { finish() }
        }

        setDefaultValues()

        lifecycleScope.launch {
            thresholdPrefs.getThreshold().collect { threshold ->
                binding.seekBarMin.progress = threshold.min
                binding.textMinCurrentValue.setText(threshold.min.toString())

                binding.seekBarMax.progress = threshold.max
                binding.textMaxCurrentValue.setText(threshold.max.toString())

                binding.seekBarDrip.progress = threshold.drip
                binding.textDripCurrentValue.setText(threshold.drip.toString())

                binding.inputHospitalName.setText(threshold.hospital)
                binding.inputPatientName.setText(threshold.user)
            }
        }

        setRequiredLabel(binding.labelMinTitle, "Batasan Minimum (ml)")
        setRequiredLabel(binding.labelMaxTitle, "Batasan Maksimum (ml)")
        setRequiredLabel(binding.labelDripTitle, "Laju Tetesan (TPM)")

        setupSeekBarWithEditText(binding.seekBarMin, binding.textMinCurrentValue)
        setupSeekBarWithEditText(binding.seekBarMax, binding.textMaxCurrentValue)
        setupSeekBarWithEditText(binding.seekBarDrip, binding.textDripCurrentValue)

        binding.buttonEditThreshold.setOnClickListener {
            val min = binding.textMinCurrentValue.text.toString().toIntOrNull() ?: 0
            val max = binding.textMaxCurrentValue.text.toString().toIntOrNull() ?: 0
            val drip = binding.textDripCurrentValue.text.toString().toIntOrNull() ?: 0
            val userName = binding.inputPatientName.text.toString()
            val hospitalName = binding.inputHospitalName.text.toString()

            if (validateThreshold(min, max, drip)) {
                lifecycleScope.launch {
                    thresholdPrefs.saveThreshold(min, max, drip, userName, hospitalName)
                    Toast.makeText(
                        this@EditThresholdActivity, "Batasan berhasil disimpan", Toast.LENGTH_SHORT
                    ).show()

                    if (isFirstTime) {
                        // Arahkan ke MainActivity dan buka HomeFragment
                        val intent = Intent(this@EditThresholdActivity, MainActivity::class.java)
                        startActivity(intent)
                    }
                    finish()
                }
            } else {
                Toast.makeText(
                    this@EditThresholdActivity,
                    "Periksa kembali nilai yang diisi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setRequiredLabel(textView: TextView, labelText: String) {
        val fullText = "$labelText *"
        val spannable = SpannableStringBuilder(fullText).apply {
            setSpan(
                ForegroundColorSpan(getColor(R.color.red)),
                fullText.length - 1,
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = spannable
    }

    private fun setDefaultValues() {
        val defaultUserName = "Pasien"
        val defaultHospitalName = "Rumah Sakit"

        if (binding.inputPatientName.text.isNullOrEmpty()) {
            binding.inputPatientName.setText(defaultUserName)
        }

        if (binding.inputHospitalName.text.isNullOrEmpty()) {
            binding.inputHospitalName.setText(defaultHospitalName)
        }
    }

    private fun showFirstTimeGreeting(isFirstTime: Boolean) {
        if (isFirstTime) {
            binding.textGreetingDescription.visibility = View.VISIBLE
            binding.textGreetingTitle.visibility = View.VISIBLE
        } else {
            binding.textGreetingDescription.visibility = View.GONE
            binding.textGreetingTitle.visibility = View.GONE
        }
    }

    private fun setupSeekBarWithEditText(seekBar: SeekBar, editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isSeekBarUpdating) return

                s?.let {
                    var text = it.toString()
                    if (text.length > 1 && text.startsWith("0")) {
                        val corrected = text.trimStart('0')
                        text = if (corrected.isEmpty()) "0" else corrected

                        val selectionStart = editText.selectionStart

                        isEditTextUpdating = true
                        editText.setText(text)

                        editText.post {
                            val newSelection = selectionStart.coerceAtMost(text.length)
                            editText.setSelection(newSelection)
                            isEditTextUpdating = false
                        }
                    }

                    val value = text.toIntOrNull() ?: 0
                    if (value in 0..seekBar.max) {
                        isEditTextUpdating = true
                        seekBar.progress = value
                        isEditTextUpdating = false
                    }

                    validateAllInputs()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isEditTextUpdating) return

                if (fromUser) {
                    isSeekBarUpdating = true
                    editText.setText(progress.toString())

                    editText.setSelection(editText.text.length)
                    isSeekBarUpdating = false

                    validateAllInputs()
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun validateAllInputs() {
        val min = binding.textMinCurrentValue.text.toString().toIntOrNull() ?: 0
        val max = binding.textMaxCurrentValue.text.toString().toIntOrNull() ?: 0
        val drip = binding.textDripCurrentValue.text.toString().toIntOrNull() ?: 0

        when {
            min <= 0 -> {
                binding.textErrorMin.text = "Nilai minimum tidak boleh 0"
            }

            min > 500 -> {
                binding.textErrorMin.text = "Nilai minimum tidak boleh lebih dari 500"
            }

            else -> {
                binding.textErrorMin.text = ""
            }
        }

        when {
            max <= 0 -> {
                binding.textErrorMax.text = "Nilai maksimum tidak boleh 0"
            }

            max > 500 -> {
                binding.textErrorMax.text = "Nilai maksimum tidak boleh lebih dari 500"
            }

            min < max -> {
                binding.textErrorMax.text = "Nilai maksimum tidak boleh lebih besar dari minimum"
            }

            else -> {
                binding.textErrorMax.text = ""
            }
        }

        when {
            drip <= 0 -> {
                binding.textErrorDrip.text = "Laju tetesan tidak boleh 0"
            }

            drip > 150 -> {
                binding.textErrorDrip.text = "Laju tetesan terlalu tinggi (maks 120 tpm)"
            }

            else -> {
                binding.textErrorDrip.text = ""
            }
        }
    }

    private fun validateThreshold(min: Int, max: Int, drip: Int): Boolean {
        var valid = true

        if (min < max || min <= 0 || max == 0 || max > 500 || min > 500) valid = false
        if (drip <= 0 || drip > 150) valid = false

        return valid
    }
}
