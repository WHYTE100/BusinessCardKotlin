package io.pridetechnologies.businesscard

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.airbnb.paris.Paris
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import io.pridetechnologies.businesscard.activities.NewBusinessCardActivity
import io.pridetechnologies.businesscard.activities.NewCardActivity
import io.pridetechnologies.businesscard.databinding.CustomAddCardBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.fragments.BusinessesHomeFragment
import io.pridetechnologies.businesscard.fragments.IndividualsHomeFragment
import java.io.FileNotFoundException
import java.io.InputStream


class HomeActivity : AppCompatActivity() {

    private var cardKey: String? = null
    private var cardKey2: String? = null
    private var UID: String? = null
    private var qrScan: IntentIntegrator? = null
    private var result: IntentResult? = null
    private var mCompressor: FileCompressor? = null
    var progressDialog: ProgressDialog? = null
    private var indiImage: ImageView? = null
    private var bizImage: ImageView? = null
    private var indiText: TextView? = null
    private var bizText: TextView? = null
    private var mAuth: FirebaseAuth? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                    val path = deepLink?.path
                    if (path != null) {
                        // Determine the appropriate activity based on the deep link path
                        when {
                            path.contains("/individuals") -> {
                                val uid = deepLink?.getQueryParameter("key")
                                val intent1 = Intent(this, NewCardActivity::class.java)
                                intent1.putExtra("deepLink", uid.toString())
                                startActivity(intent1)
                            }
                            path.contains("/businesses") -> {
                                val uid = deepLink?.getQueryParameter("key")
                                val intent2 = Intent(this, NewBusinessCardActivity::class.java)
                                intent2.putExtra("business_id", uid.toString())
                                startActivity(intent2)
                            }
                            // Add more cases for different paths if needed
                            else -> {
                                // Handle unknown paths or default behavior
                            }
                        }
                    }
                }
            }
            .addOnFailureListener(this) {
                // Handle any errors here.
            }


        mAuth = FirebaseAuth.getInstance()
        checkUserStatus()
        indiText = findViewById<View>(R.id.textView104) as TextView?
        bizText = findViewById<View>(R.id.textView105) as TextView?
        indiImage = findViewById<View>(R.id.imageView29) as ImageView?
        bizImage = findViewById<View>(R.id.imageView32) as ImageView?
        if (savedInstanceState == null) {
            openFragment(IndividualsHomeFragment())
            Paris.style(indiImage).apply(R.style.ImageViewTint)
            Paris.style(bizImage).apply(R.style.ImageViewTintClear)
        }
        progressDialog = ProgressDialog(this)

        mCompressor = FileCompressor(this)
    }

    private fun checkUserStatus() {
        val user: FirebaseUser? = mAuth?.currentUser
        if (user != null) {
            UID = user.uid
            val sp: SharedPreferences = getSharedPreferences("SP_USER", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sp.edit()
            editor.putString("Current_USERID", UID)
            editor.apply()
        }
    }

    private fun openFragment(fragment: Fragment?) {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment!!)
        fragmentTransaction.commit()
    }

    fun openBusinessesFrag(view: View?) {
        openBizFragment(BusinessesHomeFragment())
        Paris.style(indiImage).apply(R.style.ImageViewTintClear)
        Paris.style(bizImage).apply(R.style.ImageViewTint)
    }

    private fun openBizFragment(businessesHomeFragment: BusinessesHomeFragment) {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, businessesHomeFragment)
        fragmentTransaction.commit()
    }

    fun openIndividualsFrag(view: View?) {
        openIndiFrag(IndividualsHomeFragment())
        Paris.style(indiImage).apply(R.style.ImageViewTint)
        Paris.style(bizImage).apply(R.style.ImageViewTintClear)
    }

    private fun openIndiFrag(individualHomeFragment: IndividualsHomeFragment) {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, individualHomeFragment)
        fragmentTransaction.commit()
    }

    fun scanCode(view: View?) {
        val dialog = Dialog(this)
        val dialogBinding: CustomAddCardBinding = CustomAddCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.scanIndividualButton.setOnClickListener {
            qrScan = IntentIntegrator(this@HomeActivity)
            qrScan!!.setOrientationLocked(true)
            qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            qrScan!!.setPrompt("Scan Individual QR Code")
            qrScan!!.setCameraId(0) // Use a specific camera of the device
            qrScan!!.setBeepEnabled(false)
            qrScan!!.setBarcodeImageEnabled(true)
            qrScan!!.initiateScan()
        }
        dialogBinding.galleyIndividualButton.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*"
            )
            startActivityForResult(pickIntent, REQUEST_GALLERY_PHOTO)
        }
        dialogBinding.scanBusinessButton.setOnClickListener {
            val intent = Intent(this@HomeActivity, ScanBusinessActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        dialogBinding.galleyBusinessButton.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*"
            )
            startActivityForResult(pickIntent, BUSINESS_REQUEST_GALLERY_PHOTO)
        }
        dialogBinding.button10.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(true)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        //result2 = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && resultCode == Activity.RESULT_OK && data != null) {
            if (result!!.contents == null) {
                //Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                cardKey = result!!.contents.toString().trim { it <= ' ' }
                val intent = Intent(this@HomeActivity, NewCardActivity::class.java)
                intent.putExtra("deepLink", cardKey)
                startActivity(intent)
                Animatoo.animateFade(this@HomeActivity)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
        if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
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
                    cardKey = result.text.toString().trim { it <= ' ' }
                    val intent = Intent(this@HomeActivity, NewCardActivity::class.java)
                    intent.putExtra("deepLink", cardKey)
                    startActivity(intent)
                    Animatoo.animateFade(this@HomeActivity)
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
                    cardKey2 = result.text.toString().trim { it <= ' ' }
                    val intent = Intent(this@HomeActivity, NewBusinessCardActivity::class.java)
                    intent.putExtra("business_id", cardKey2)
                    startActivity(intent)
                    Animatoo.animateFade(this@HomeActivity)
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
        const val REQUEST_GALLERY_PHOTO = 8
        const val BUSINESS_REQUEST_GALLERY_PHOTO = 20
    }
}