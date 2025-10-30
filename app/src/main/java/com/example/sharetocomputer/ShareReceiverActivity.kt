package com.example.sharetocomputer

import android.app.ProgressDialog
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class ShareReceiverActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processSharedFile()
    }
    
    private fun processSharedFile() {
        val intent = intent
        val action = intent.action
        val type = intent.type
        
        if (Intent.ACTION_SEND == action && type != null) {
            val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (fileUri != null) {
                FileSendTask().execute(fileUri)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
    
    private inner class FileSendTask : AsyncTask<Uri, Void, Boolean>() {
        private lateinit var progressDialog: ProgressDialog
        private var fileName: String = ""
        
        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog(this@ShareReceiverActivity)
            progressDialog.setMessage("در حال ارسال فایل به کامپیوتر...")
            progressDialog.setCancelable(false)
            progressDialog.show()
        }
        
        override fun doInBackground(vararg params: Uri): Boolean {
            return try {
                val fileUri = params[0]
                fileName = getFileName(fileUri) ?: "file_${System.currentTimeMillis()}"
                
                val tempFile = saveFileToTemp(fileUri, fileName)
                if (tempFile == null) return false
                
                sendViaADB(tempFile, fileName)
                tempFile.delete()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        override fun onPostExecute(success: Boolean) {
            progressDialog.dismiss()
            if (success) {
                Toast.makeText(this@ShareReceiverActivity, "فایل \"$fileName\" با موفقیت ارسال شد", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ShareReceiverActivity, "خطا در ارسال فایل", Toast.LENGTH_LONG).show()
            }
            finish()
        }
        
        private fun saveFileToTemp(uri: Uri, fileName: String): File? {
            return try {
                val tempDir = File(cacheDir, "temp_files")
                if (!tempDir.exists()) tempDir.mkdirs()
                
                val tempFile = File(tempDir, fileName)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        
        private fun sendViaADB(file: File, fileName: String): Boolean {
            return try {
                val pcPath = "/sdcard/Download/"
                val command = "adb push \"${file.absolutePath}\" \"$pcPath$fileName\""
                Runtime.getRuntime().exec(command).waitFor()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        private fun getFileName(uri: Uri): String? {
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        result = it.getString(it.getColumnIndex("_display_name"))
                    }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != -1) {
                    result = result?.substring(cut!! + 1)
                }
            }
            return result
        }
    }
}
