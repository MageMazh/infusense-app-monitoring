package com.d121211069.infusense.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d121211069.infusense.data.ThresholdItem
import com.d121211069.infusense.data.local.entity.HistoryEntity
import com.d121211069.infusense.data.repository.HistoryRepository
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val thresholdPrefs: ThresholdPreferences, private val repository: HistoryRepository
) : ViewModel() {

    fun insert(item: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(item)
        }
    }

    val thresholdFlow: StateFlow<ThresholdItem> = thresholdPrefs.getThreshold().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ThresholdItem(0, 0, 0)
    )

    private var hasReachedMin = false
    private var hasReachedCritical = false
    var currentMin: Int = 0
    var currentMax: Int = 0
    var targetTPM: Int = 0

    fun resetStatusFlags() {
        hasReachedMin = false
        hasReachedCritical = false
    }

    private val _volume = MutableLiveData<Double>()
    val volume: LiveData<Double> = _volume

    private val _tpm = MutableLiveData<Int>()
    val tpm: LiveData<Int> = _tpm

    private val _interval = MutableLiveData<Double>()
    val interval: LiveData<Double> = _interval

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private var listener: ValueEventListener? = null
    private var dbRef: DatabaseReference? = null

    var currentStatus: String = "initializing"

    fun observeData(roomId: String) {
        if (listener != null && dbRef != null) {
            dbRef?.removeEventListener(listener!!)
        }

        dbRef = FirebaseDatabase.getInstance().getReference("patients").child(roomId)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _status.postValue(
                    snapshot.child("status").getValue(String::class.java) ?: "unknown"
                )
                _volume.postValue(snapshot.child("volume").getValue(Double::class.java) ?: 0.0)
                _interval.postValue(snapshot.child("interval").getValue(Double::class.java) ?: 0.0)
                _tpm.postValue(snapshot.child("drip_rate_tpm").getValue(Int::class.java) ?: 0)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "Firebase error: ${error.message}")
            }
        }

        dbRef?.addValueEventListener(listener!!)
    }

    fun stopObserving() {
        dbRef?.let { ref ->
            listener?.let { ref.removeEventListener(it) }
        }
        listener = null
        dbRef = null
    }

    override fun onCleared() {
        super.onCleared()
        dbRef?.removeEventListener(listener!!)
    }
}
