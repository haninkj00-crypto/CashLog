package com.example.cashlog.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cashlog.model.Denomination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val DEFAULT_ADJUSTMENT_PRESET_KEY = stringPreferencesKey("default_adjustment_preset")

    val defaultAdjustmentPreset: Flow<Map<Denomination, Int>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[DEFAULT_ADJUSTMENT_PRESET_KEY] ?: "{}"
            parseJsonToMap(jsonString)
        }

    suspend fun saveDefaultAdjustmentPreset(preset: Map<Denomination, Int>) {
        val jsonString = mapToJson(preset)
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_ADJUSTMENT_PRESET_KEY] = jsonString
        }
    }

   private fun parseJsonToMap(jsonString: String): Map<Denomination, Int> {
       val map = Denomination.values().associateWith { 0 }.toMutableMap()
       try {
           val json = JSONObject(jsonString)
           val keys = json.keys()
           while (keys.hasNext()) {
               val key = keys.next()
               val denom = Denomination.valueOf(key)
               val count = json.getInt(key)
               map[denom] = count
           }
       } catch (e: Exception) {
           // Keep the all-zero defaults if parsing fails
       }
       return map
   }
    private fun mapToJson(map: Map<Denomination, Int>): String {
        val json = JSONObject()
        map.forEach { (denom, count) ->
            json.put(denom.name, count)
        }
        return json.toString()
    }
}
