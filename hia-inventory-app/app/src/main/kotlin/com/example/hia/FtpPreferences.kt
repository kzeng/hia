package com.example.hia

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Data model for FTP configuration
data class FtpConfig(
    val server: String = "",
    val port: Int = 21,
    val user: String = "",
    val password: String = ""
)

// Preferences DataStore instance bound to Context
val Context.ftpDataStore by preferencesDataStore(name = "ftp_prefs")

object FtpPreferences {
    private val KEY_SERVER = stringPreferencesKey("ftp_server")
    private val KEY_PORT = intPreferencesKey("ftp_port")
    private val KEY_USER = stringPreferencesKey("ftp_user")
    private val KEY_PASSWORD = stringPreferencesKey("ftp_password")

    fun getConfig(context: Context): Flow<FtpConfig> = context.ftpDataStore.data.map { prefs ->
        FtpConfig(
            server = prefs[KEY_SERVER] ?: "",
            port = prefs[KEY_PORT] ?: 21,
            user = prefs[KEY_USER] ?: "",
            password = prefs[KEY_PASSWORD] ?: ""
        )
    }

    suspend fun saveConfig(context: Context, cfg: FtpConfig) {
        context.ftpDataStore.edit { prefs ->
            prefs[KEY_SERVER] = cfg.server
            prefs[KEY_PORT] = cfg.port
            prefs[KEY_USER] = cfg.user
            prefs[KEY_PASSWORD] = cfg.password
        }
    }
}
