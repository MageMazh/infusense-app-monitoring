package com.d121211069.infusense.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.d121211069.infusense.data.ThresholdItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.thresholdDataStore: DataStore<Preferences> by preferencesDataStore(name = "thresholds")

class ThresholdPreferences private constructor(private val dataStore: DataStore<Preferences>) {

    suspend fun saveThreshold(min: Int, max: Int, drip: Int, user: String, hospital: String) {
        dataStore.edit { prefs ->
            prefs[MIN_KEY] = min
            prefs[MAX_KEY] = max
            prefs[DRIP_KEY] = drip
            prefs[USER_KEY] = user
            prefs[HOSPITAL_KEY] = hospital
        }
    }

    fun getThreshold(): Flow<ThresholdItem> {
        return dataStore.data.map { prefs ->
            ThresholdItem(
                min = prefs[MIN_KEY] ?: 0,
                max = prefs[MAX_KEY] ?: 0,
                drip = prefs[DRIP_KEY] ?: 0,
                user = prefs[USER_KEY] ?: "-",
                hospital = prefs[HOSPITAL_KEY] ?: "-"
            )
        }
    }

    suspend fun getRoomId(): String {
        val prefs = dataStore.data.first()
        return prefs[ROOM_KEY] ?: ""
    }

    suspend fun saveRoomId(roomId: String) {
        dataStore.edit { prefs -> prefs[ROOM_KEY] = roomId }
    }

    suspend fun clearRoomId() {
        dataStore.edit { prefs -> prefs.remove(ROOM_KEY) }
    }

    fun reachedMinFlow(): Flow<Boolean> = dataStore.data.map { it[REACHED_MIN_KEY] ?: false }
    fun reachedCritFlow(): Flow<Boolean> = dataStore.data.map { it[REACHED_CRIT_KEY] ?: false }

    fun lastSpeedNotifyAtFlow(): Flow<Long> =
        dataStore.data.map { it[LAST_SPEED_NOTIFY_AT_MS_KEY] ?: 0L }

    fun lastNoDripAlarmAtFlow(): Flow<Long> =
        dataStore.data.map { it[LAST_NO_DRIP_ALARM_AT_MS_KEY] ?: 0L }

    fun reachedNoDripFlow(): Flow<Boolean> = dataStore.data.map { it[REACHED_NO_DRIP_KEY] ?: false }

    suspend fun setReachedMin(v: Boolean) {
        dataStore.edit { it[REACHED_MIN_KEY] = v }
    }

    suspend fun setReachedCrit(v: Boolean) {
        dataStore.edit { it[REACHED_CRIT_KEY] = v }
    }

    suspend fun setLastMinNotifyAt(ms: Long) {
        dataStore.edit { it[LAST_MIN_NOTIFY_AT_MS_KEY] = ms }
    }

    suspend fun setLastSpeedNotifyAt(ms: Long) {
        dataStore.edit { it[LAST_SPEED_NOTIFY_AT_MS_KEY] = ms }
    }

    suspend fun setReachedNoDrip(v: Boolean) {
        dataStore.edit { it[REACHED_NO_DRIP_KEY] = v }
    }

    suspend fun setLastNoDripAlarmAt(ms: Long) {
        dataStore.edit { it[LAST_NO_DRIP_ALARM_AT_MS_KEY] = ms }
    }

    companion object {
        @Volatile
        private var INSTANCE: ThresholdPreferences? = null

        private val MIN_KEY = intPreferencesKey("min")
        private val MAX_KEY = intPreferencesKey("max")
        private val DRIP_KEY = intPreferencesKey("drip")
        private val USER_KEY = stringPreferencesKey("user")
        private val HOSPITAL_KEY = stringPreferencesKey("hospital")
        private val ROOM_KEY = stringPreferencesKey("room_id")

        private val REACHED_MIN_KEY = booleanPreferencesKey("reached_min")
        private val REACHED_CRIT_KEY = booleanPreferencesKey("reached_crit")
        private val LAST_MIN_NOTIFY_AT_MS_KEY = longPreferencesKey("last_min_notify_at_ms")
        private val LAST_SPEED_NOTIFY_AT_MS_KEY = longPreferencesKey("last_speed_notify_at_ms")
        private val LAST_NO_DRIP_ALARM_AT_MS_KEY = longPreferencesKey("last_no_drip_alarm_at_ms")
        private val REACHED_NO_DRIP_KEY = booleanPreferencesKey("reached_no_drip")

        fun getInstance(dataStore: DataStore<Preferences>): ThresholdPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = ThresholdPreferences(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
