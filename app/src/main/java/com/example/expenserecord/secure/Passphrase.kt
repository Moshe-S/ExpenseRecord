package com.example.expenserecord.secure

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import java.security.SecureRandom

object Passphrase {
    private const val PREF_FILE = "secure_prefs"
    private const val KEY_NAME = "db_key"

    fun get(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_NAME, null)
        val keyString = existing ?: run {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            val s = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_NAME, s).apply()
            s
        }
        return SQLiteDatabase.getBytes(keyString.toCharArray())
    }
}
