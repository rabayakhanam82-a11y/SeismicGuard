package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.model.EarthquakeItem
import org.json.JSONArray
import org.json.JSONObject

class SeismicOfflineCacheManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("seismic_offline_cache_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SeismicOfflineCache"
        private const val KEY_CACHED_EARTHQUAKES = "cached_earthquakes_json"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_IS_OFFLINE_MODE = "is_offline_mode"

        @Volatile
        private var INSTANCE: SeismicOfflineCacheManager? = null

        fun getInstance(context: Context): SeismicOfflineCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeismicOfflineCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun saveEarthquakesToCache(items: List<EarthquakeItem>) {
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("magnitude", item.magnitude)
                    put("place", item.place)
                    put("timeMillis", item.timeMillis)
                    put("latitude", item.latitude)
                    put("longitude", item.longitude)
                    put("depthKm", item.depthKm)
                    put("distanceKmFromUser", item.distanceKmFromUser)
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_CACHED_EARTHQUAKES, jsonArray.toString())
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .putBoolean(KEY_IS_OFFLINE_MODE, false)
                .apply()
            Log.d(TAG, "Cached ${items.size} earthquakes to offline persistent store")
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving earthquakes to offline cache", e)
        }
    }

    fun getCachedEarthquakes(): List<EarthquakeItem> {
        val raw = prefs.getString(KEY_CACHED_EARTHQUAKES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<EarthquakeItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    EarthquakeItem(
                        id = obj.optString("id", "quake_$i"),
                        magnitude = obj.optDouble("magnitude", 3.0),
                        place = obj.optString("place", "Seismic Zone"),
                        timeMillis = obj.optLong("timeMillis", System.currentTimeMillis()),
                        latitude = obj.optDouble("latitude", 23.8),
                        longitude = obj.optDouble("longitude", 90.4),
                        depthKm = obj.optDouble("depthKm", 10.0),
                        distanceKmFromUser = obj.optDouble("distanceKmFromUser", 0.0)
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing cached earthquakes", e)
            emptyList()
        }
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun markOfflineMode(isOffline: Boolean) {
        prefs.edit().putBoolean(KEY_IS_OFFLINE_MODE, isOffline).apply()
    }

    fun isOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_IS_OFFLINE_MODE, false)
    }

    fun clearOfflineCacheOnNetworkRestore() {
        prefs.edit()
            .putBoolean(KEY_IS_OFFLINE_MODE, false)
            .apply()
        Log.d(TAG, "Cleared offline mode flag on network restoration")
    }

    fun registerNetworkCallback(onRestored: () -> Unit) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network connection restored - Triggering sync & clearing stale offline state")
                    onRestored()
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "Network disconnected - Switching to offline cached data")
                    markOfflineMode(true)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering network callback", e)
        }
    }
}
