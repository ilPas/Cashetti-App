package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.backup.BudgetBackupData
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class DriveBackupResult {
    data class Success(val message: String, val timestampMillis: Long = System.currentTimeMillis()) : DriveBackupResult()
    data class Error(val message: String, val recoveryIntent: Intent? = null) : DriveBackupResult()
}

sealed class DriveRestoreResult {
    data class Success(val backupData: BudgetBackupData, val infoMessage: String) : DriveRestoreResult()
    data class Error(val message: String, val recoveryIntent: Intent? = null) : DriveRestoreResult()
}

object GoogleDriveBackupService {
    private const val TAG = "GoogleDriveBackup"
    private const val BACKUP_FILENAME = "budget_control_backup.json"
    private const val DRIVE_SCOPES = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val account = getSignedInAccount(context) ?: return@withContext null
        try {
            val accountObject = account.account ?: return@withContext null
            GoogleAuthUtil.getToken(context, accountObject, DRIVE_SCOPES)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}", e)
            null
        }
    }

    /**
     * Uploads the backup JSON to Google Drive (either creates or overwrites the backup file).
     */
    suspend fun uploadBackup(
        context: Context, 
        backupData: BudgetBackupData,
        onProgress: ((Float) -> Unit)? = null
    ): DriveBackupResult = withContext(Dispatchers.IO) {
        val account = getSignedInAccount(context)
        if (account == null) {
            return@withContext DriveBackupResult.Error("Nessun account Google collegato. Effettua l'accesso prima del backup.")
        }

        val token = try {
            getAccessToken(context)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            return@withContext DriveBackupResult.Error("Autorizzazione mancante per Google Drive.", e.intent)
        }

        if (token == null) {
            return@withContext DriveBackupResult.Error("Impossibile ottenere l'autorizzazione Google Drive. Riprova ad accedere.")
        }

        try {
            val jsonPayload = backupData.toJson()
            val existingFileId = findBackupFileId(token)

            val success = if (existingFileId != null) {
                updateFile(token, existingFileId, jsonPayload)
            } else {
                createFile(token, BACKUP_FILENAME, jsonPayload)
            }

            if (success) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateStr = formatter.format(Date(backupData.exportedAtMillis))
                DriveBackupResult.Success(
                    message = "Backup sincronizzato con successo su Google Drive ($dateStr).",
                    timestampMillis = backupData.exportedAtMillis
                )
            } else {
                DriveBackupResult.Error("Errore durante il caricamento su Google Drive.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during backup upload", e)
            DriveBackupResult.Error("Eccezione durante il backup: ${e.localizedMessage}")
        }
    }

    /**
     * Downloads and parses the latest backup JSON from Google Drive.
     */
    suspend fun downloadBackup(
        context: Context,
        onProgress: ((Float) -> Unit)? = null
    ): DriveRestoreResult = withContext(Dispatchers.IO) {
        val account = getSignedInAccount(context)
        if (account == null) {
            return@withContext DriveRestoreResult.Error("Nessun account Google collegato.")
        }

        val token = try {
            getAccessToken(context)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            return@withContext DriveRestoreResult.Error("Autorizzazione mancante per Google Drive.", e.intent)
        }

        if (token == null) {
            return@withContext DriveRestoreResult.Error("Impossibile autenticare Google Drive.")
        }

        try {
            val fileId = findBackupFileId(token)
                ?: return@withContext DriveRestoreResult.Error("Nessun file di backup 'budget_control_backup.json' trovato su Google Drive.")

            val getUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(getUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext DriveRestoreResult.Error("Download fallito con codice ${response.code}: ${response.message}")
            }

            val jsonString = response.body?.string()
                ?: return@withContext DriveRestoreResult.Error("File di backup vuoto.")

            val backupData = BudgetBackupData.fromJson(jsonString)
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = formatter.format(Date(backupData.exportedAtMillis))

            DriveRestoreResult.Success(
                backupData = backupData,
                infoMessage = "Trovato backup del $dateStr contenente ${backupData.expenses.size} spese, ${backupData.subscriptions.size} abbonamenti e ${backupData.categories.size} categorie."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup", e)
            DriveRestoreResult.Error("Errore durante il download del backup: ${e.localizedMessage}")
        }
    }

    private fun findBackupFileId(token: String): String? {
        val queryUrl = "https://www.googleapis.com/drive/v3/files?spaces=drive,appDataFolder&q=name%3D'$BACKUP_FILENAME'+and+trashed%3Dfalse&fields=files(id,name,modifiedTime)"
        val request = Request.Builder()
            .url(queryUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Query backup file failed: ${response.code} ${response.message}")
            return null
        }

        val bodyString = response.body?.string() ?: return null
        val json = JSONObject(bodyString)
        val files = json.optJSONArray("files")
        if (files != null && files.length() > 0) {
            return files.getJSONObject(0).optString("id")
        }
        return null
    }

    private fun createFile(token: String, filename: String, contentJson: String): Boolean {
        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

        val metadataJson = JSONObject().apply {
            put("name", filename)
            put("description", "Budget Control App Backup")
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                contentJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val success = response.isSuccessful
        if (!success) {
            Log.e(TAG, "Create file failed: ${response.code} ${response.body?.string()}")
        }
        return success
    }

    private fun updateFile(token: String, fileId: String, contentJson: String): Boolean {
        val url = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val requestBody = contentJson.toRequestBody("application/json; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .patch(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val success = response.isSuccessful
        if (!success) {
            Log.e(TAG, "Update file failed: ${response.code} ${response.body?.string()}")
        }
        return success
    }
}
