package io.pridetechnologies.businesscard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.pridetechnologies.businesscard.notifications.PushNotification
import io.pridetechnologies.businesscard.notifications.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class Constants {
    val auth = FirebaseAuth.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    var storageRef = Firebase.storage.reference

    fun setSnackBar(context: Context, message: String){
        val snackBar = Snackbar.make(View(context), message, Snackbar.LENGTH_LONG)
        snackBar.show()
    }

    fun generateQRCodeWithIcon(content: String, icon: Bitmap, size: Int): Bitmap? {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = 0 // Set QR code margin to 0

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height

        // Create a bitmap from the QR code byte array
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        // Set the QR code colors to yellow and black
        val backgroundPaint = Paint()
        backgroundPaint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val foregroundPaint = Paint()
        foregroundPaint.color = Color.BLACK

        // Draw the QR code modules onto the canvas
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (bitMatrix[x, y]) {
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), foregroundPaint)
                }
            }
        }

        // Add the icon to the center of the QR code
        val centerX = width / 2f
        val centerY = height / 2f
        val iconSize = width * 0.2f // Set the size of the icon as a fraction of the QR code size
        val iconRect = RectF(centerX - iconSize / 2, centerY - iconSize / 2, centerX + iconSize / 2, centerY + iconSize / 2)
        canvas.drawBitmap(icon, null, iconRect, null)

        return bitmap
    }

    fun openProfileInApp(context: Context, packageName: String, profileLink: String) {
        val pm: PackageManager = context.packageManager
        val isAppInstalled: Boolean = try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException){
            false
        }
        if (profileLink.equals("null")){
            Toast.makeText(context, "Link not set", Toast.LENGTH_SHORT).show()
        }else{
            //Toast.makeText(context, "Link: $profileLink", Toast.LENGTH_SHORT).show()
            if (isAppInstalled){
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profileLink))
                    intent.setPackage(packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception){
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profileLink))
                    context.startActivity(intent)
                }

            }else{
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profileLink))
                context.startActivity(intent)
            }
        }
    }
    fun openNumberInWhatsApp(context: Context, packageName: String, whatsAppNumber: String) {
        val pm: PackageManager = context.packageManager
        val isAppInstalled: Boolean = try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException){
            false
        }
        if (whatsAppNumber.equals("null")){
            Toast.makeText(context, "Link not set", Toast.LENGTH_SHORT).show()
        }else{
            //Toast.makeText(context, "Link: $profileLink", Toast.LENGTH_SHORT).show()
            if (isAppInstalled){

                val url = "https://api.whatsapp.com/send?phone=$whatsAppNumber"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.setPackage(packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }else{
                Toast.makeText(context, "No WhatsApp on your device", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openProfileInFaceBookApp(context: Context, facebookLink: String): Intent? {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.facebook.katana", 0)
            Intent(Intent.ACTION_VIEW, Uri.parse(facebookLink))
        } catch (e: Exception){
            Intent(Intent.ACTION_VIEW, Uri.parse(facebookLink))
        }
    }

    // Function to write data to SharedPreferences
    fun writeToSharedPreferences(context: Context, key: String, value: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("BusinessCard", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    // Function to read data from SharedPreferences
    fun readFromSharedPreferences(context: Context, key: String, defaultValue: String): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("BusinessCard", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun downloadCode(context: Context, urlString: String): Bitmap? {
        val url: URL = mStringToURL(urlString)!!
        val connection: HttpURLConnection?
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream: InputStream = connection.inputStream
            val bufferedInputStream = BufferedInputStream(inputStream)
            return BitmapFactory.decodeStream(bufferedInputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            showToast(context, "Error")
        }
        return null
    }
    private fun mStringToURL(string: String): URL? {
        try {
            return URL(string)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return null
    }

    fun showToast(context: Context, message: String) {

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    fun saveMediaToStorage(context: Context, bitmap: Bitmap?, fileName: String) {
        val filename = "${fileName}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
            showToast(context, "Saved to Gallery")
        }
    }

    fun copyText(context: Context, textString: String){
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Link", textString)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }

    fun openGoogleMapsAndDirections(context: Context, latitude: Double, longitude: Double) {
        val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            showToast(context, "Can't open maps now.")
            // Handle the case when Google Maps app is not installed
            // You can open a web-based map instead, or display an error message
        }
    }
    fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful){
                //Log.d(TAG, "Response: ${response}")
            }else{
                //Log.e(TAG, response.errorBody().toString())
            }
        }catch (e: Exception){
            //Log.e(TAG, e.toString())
        }
    }
}