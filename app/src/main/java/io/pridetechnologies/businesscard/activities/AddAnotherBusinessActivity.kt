package io.pridetechnologies.businesscard.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import io.pridetechnologies.businesscard.ExistingBusinessActivity
import io.pridetechnologies.businesscard.HomeActivity
import io.pridetechnologies.businesscard.databinding.ActivityAddAnotherBusinessBinding
import io.pridetechnologies.businesscard.databinding.CustomLinkBusinessCardBinding
import java.io.FileNotFoundException
import java.io.InputStream

class AddAnotherBusinessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddAnotherBusinessBinding
    private var businessId: String? = null
    private var qrScan: IntentIntegrator? = null
    private var result: IntentResult? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAnotherBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.addNewButton.setOnClickListener {
            val intent = Intent(this, NewBusinessActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.linkButton.setOnClickListener {
            linkBusiness()
        }
    }
    private fun linkBusiness() {
        val dialog = Dialog(this)
        val dialogBinding: CustomLinkBusinessCardBinding = CustomLinkBusinessCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.scanBusinessButton.setOnClickListener {
            qrScan = IntentIntegrator(this)
            qrScan!!.setOrientationLocked(true)
            qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            qrScan!!.setPrompt("Scan QR code")
            qrScan!!.setCameraId(0) // Use a specific camera of the device
            qrScan!!.setBeepEnabled(true)
            qrScan!!.setBarcodeImageEnabled(true)
            qrScan!!.initiateScan()
        }
        dialogBinding.galleyBusinessButton.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            startActivityForResult(pickIntent, HomeActivity.BUSINESS_REQUEST_GALLERY_PHOTO)
        }
        dialogBinding.button10.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(true)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && resultCode == Activity.RESULT_OK && data != null) {
            if (result!!.contents == null) {
                //Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                businessId = result!!.contents.toString().trim { it <= ' ' }
                val intent = Intent(this, ExistingBusinessActivity::class.java)
                intent.putExtra("business_id", businessId)
                startActivity(intent)
                Animatoo.animateFade(this)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == BUSINESS_REQUEST_GALLERY_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(selectedImage!!)
                var bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.e("TAG", "uri is not a bitmap,$selectedImage")
                    return
                }
                val width: Int = bitmap.width
                val height: Int = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()
                bitmap = null
                val source = RGBLuminanceSource(width, height, pixels)
                val bBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                try {
                    val result: Result = reader.decode(bBitmap)
                    businessId = result.text.toString().trim { it <= ' ' }
                    val intent = Intent(this, ExistingBusinessActivity::class.java)
                    intent.putExtra("business_id", businessId)
                    startActivity(intent)
                    //Animatoo.animateFade(this@HomeActivity2)
                } catch (e: NotFoundException) {
                    // Toast.makeText(this, "This Code is NOT VALID", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "decode exception", e)
                }
            } catch (e: FileNotFoundException) {
                //Log.e("TAG", "can not open file" + selectedImage.toString(), e);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            //Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    companion object {
        const val BUSINESS_REQUEST_GALLERY_PHOTO = 20
    }
}