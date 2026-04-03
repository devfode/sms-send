package com.smsrelay.core.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.smsrelay.core.model.SmsRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SmsRecordStore(private val context: Context) {
    
    companion object {
        private const val PREFS_FILE = "encrypted_prefs_sms_records"
        private const val KEY_SMS_RECORDS = "sms_records"
        private const val MAX_RECORDS = 1000 // 最多保存1000条记录
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            android.util.Log.w("SmsRecordStore", "Failed to create encrypted preferences, using regular preferences", e)
            context.getSharedPreferences(PREFS_FILE + "_fallback", android.content.Context.MODE_PRIVATE)
        }
    }
    
    suspend fun saveRecord(record: SmsRecord) = withContext(Dispatchers.IO) {
        try {
            val records = getAllRecords().toMutableList()
            
            // 检查是否已存在相同ID的记录
            val existingIndex = records.indexOfFirst { it.id == record.id }
            if (existingIndex >= 0) {
                records[existingIndex] = record // 更新现有记录
            } else {
                records.add(0, record) // 添加到列表开头（最新的在前面）
                
                // 保持记录数量在限制内
                if (records.size > MAX_RECORDS) {
                    records.removeLastOrNull()
                }
            }
            
            saveRecordsToPrefs(records)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getAllRecords(): List<SmsRecord> = withContext(Dispatchers.IO) {
        try {
            val recordsJson = sharedPreferences.getString(KEY_SMS_RECORDS, null) ?: return@withContext emptyList()
            val jsonArray = JSONArray(recordsJson)
            val records = mutableListOf<SmsRecord>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val record = jsonToRecord(jsonObject)
                records.add(record)
            }
            
            records
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getRecordById(id: String): SmsRecord? = withContext(Dispatchers.IO) {
        getAllRecords().find { it.id == id }
    }
    
    suspend fun updateRecordStatus(id: String, status: SmsRecord.ProcessStatus, reason: String? = null, emailSent: Boolean = false, emailError: String? = null) = withContext(Dispatchers.IO) {
        try {
            val records = getAllRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            
            if (index >= 0) {
                val updatedRecord = records[index].copy(
                    status = status,
                    reason = reason,
                    emailSent = emailSent,
                    emailError = emailError
                )
                records[index] = updatedRecord
                saveRecordsToPrefs(records)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getRecentRecords(count: Int = 50): List<SmsRecord> = withContext(Dispatchers.IO) {
        getAllRecords().take(count)
    }
    
    suspend fun getRecordsByStatus(status: SmsRecord.ProcessStatus): List<SmsRecord> = withContext(Dispatchers.IO) {
        getAllRecords().filter { it.status == status }
    }
    
    suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        try {
            val records = getAllRecords().toMutableList()
            records.removeAll { it.id == id }
            saveRecordsToPrefs(records)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun clearAllRecords() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(KEY_SMS_RECORDS).apply()
    }
    
    suspend fun getStatistics(): RecordStatistics = withContext(Dispatchers.IO) {
        val records = getAllRecords()
        val today = System.currentTimeMillis() - 24 * 60 * 60 * 1000 // 24小时前
        
        RecordStatistics(
            totalRecords = records.size,
            todayRecords = records.count { it.timestamp > today },
            sentRecords = records.count { it.status == SmsRecord.ProcessStatus.SENT },
            failedRecords = records.count { it.status == SmsRecord.ProcessStatus.FAILED },
            queuedRecords = records.count { it.status == SmsRecord.ProcessStatus.QUEUED }
        )
    }
    
    private fun saveRecordsToPrefs(records: List<SmsRecord>) {
        try {
            val jsonArray = JSONArray()
            records.forEach { record ->
                jsonArray.put(recordToJson(record))
            }
            
            sharedPreferences.edit()
                .putString(KEY_SMS_RECORDS, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun recordToJson(record: SmsRecord): JSONObject {
        return JSONObject().apply {
            put("id", record.id)
            put("from", record.from)
            put("body", record.body)
            put("originalBody", record.originalBody)
            put("timestamp", record.timestamp)
            put("slotId", record.slotId)
            put("status", record.status.name)
            put("reason", record.reason)
            put("emailSent", record.emailSent)
            put("emailError", record.emailError)
            put("createdAt", record.createdAt)
        }
    }
    
    private fun jsonToRecord(jsonObject: JSONObject): SmsRecord {
        return SmsRecord(
            id = jsonObject.getString("id"),
            from = jsonObject.getString("from"),
            body = jsonObject.getString("body"),
            originalBody = jsonObject.getString("originalBody"),
            timestamp = jsonObject.getLong("timestamp"),
            slotId = if (jsonObject.has("slotId") && !jsonObject.isNull("slotId")) jsonObject.getInt("slotId") else null,
            status = SmsRecord.ProcessStatus.valueOf(jsonObject.getString("status")),
            reason = if (jsonObject.has("reason") && !jsonObject.isNull("reason")) jsonObject.getString("reason") else null,
            emailSent = jsonObject.optBoolean("emailSent", false),
            emailError = if (jsonObject.has("emailError") && !jsonObject.isNull("emailError")) jsonObject.getString("emailError") else null,
            createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
        )
    }
    
    data class RecordStatistics(
        val totalRecords: Int,
        val todayRecords: Int,
        val sentRecords: Int,
        val failedRecords: Int,
        val queuedRecords: Int
    )
}