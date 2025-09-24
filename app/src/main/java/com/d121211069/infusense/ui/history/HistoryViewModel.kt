package com.d121211069.infusense.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.d121211069.infusense.data.local.entity.HistoryEntity
import com.d121211069.infusense.data.repository.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val historyList: LiveData<List<HistoryEntity>> = repository.allHistory.asLiveData()

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }
}
