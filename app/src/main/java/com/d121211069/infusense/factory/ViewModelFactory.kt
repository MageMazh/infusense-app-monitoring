package com.d121211069.infusense.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.d121211069.infusense.data.local.room.HistoryDatabase
import com.d121211069.infusense.data.repository.HistoryRepository
import com.d121211069.infusense.datastore.ThresholdPreferences
import com.d121211069.infusense.datastore.thresholdDataStore
import com.d121211069.infusense.ui.history.HistoryViewModel
import com.d121211069.infusense.ui.home.HomeViewModel

class ViewModelFactory private constructor(
    private val prefs: ThresholdPreferences,
    private val repository: HistoryRepository
) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(prefs, repository) as T
        } else if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @Volatile
        private var instance: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory =
            instance ?: synchronized(this) {
                val prefs = ThresholdPreferences.getInstance(context.thresholdDataStore)

                val db = HistoryDatabase.getDatabase(context)
                val dao = db.historyDao()
                val repository = HistoryRepository.getInstance(dao)

                ViewModelFactory(prefs, repository).also { instance = it }
            }
    }
}
