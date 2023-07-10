package io.pridetechnologies.businesscard

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.activities.BusinessAddressActivity
import io.pridetechnologies.businesscard.databinding.ActivityEditBusinessBasicsBinding
import io.pridetechnologies.businesscard.databinding.ActivityNewBusinessBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import java.io.ByteArrayOutputStream
import java.io.File

class EditBusinessBasicsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBusinessBasicsBinding
    private val constants = Constants()
    private val progressDialog by lazy { CustomProgressDialog(this) }

    private var imageUri: Uri? = null
    private var businessId: String? = ""

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            binding.businessLogoView.setImageURI(uri)
            if (uri != null) {
                imageUri = uri
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusinessBasicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        businessId = intent.getStringExtra("business_id")

        binding.nextButton.setOnClickListener {
            saveDetails()
        }
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.changeImageButton.setOnClickListener {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                selectImage()
                            }
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        // Remember to invoke this method when the custom rationale is closed
                        // or just by default if you don't want to use any custom rationale.
                        token?.continuePermissionRequest()
                    }
                })
                .withErrorListener {
                }
                .check()
        }

        getBusinessDetails()
    }

    private fun getBusinessDetails() {
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                val businessLogo = snapshot?.get("business_logo").toString()
                val businessName = snapshot?.get("business_name").toString()
                val businessBio = snapshot?.get("business_bio").toString()
                val businessMobile = snapshot?.get("business_mobile").toString()
                val businessWebsite = snapshot?.get("business_website").toString()
                val businessEmail = snapshot?.get("business_email").toString()


                Picasso.get().load(businessLogo).fit().centerCrop().placeholder(R.drawable.background_icon).into(binding.businessLogoView)
                binding.businessName.setText(businessName)
                binding.businessBio.setText(businessBio)
                binding.mobile.setText(businessMobile)
                binding.businessWebsite.setText(businessWebsite)
                binding.businessEmail.setText(businessEmail)


            }
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        getGalleryImage.launch(intent)
    }

    private fun saveDetails() {
        val businessName = binding.businessName.text.toString().trim()
        val businessEmail = binding.businessEmail.text.toString().trim()
        val businessWebsite = binding.businessWebsite.text.toString().trim()
        val businessBio = binding.businessBio.text.toString().trim()
        val mobile = binding.mobile.text.toString().trim()

        if (businessName.isEmpty()){
            Toast.makeText(this, "Enter business name.", Toast.LENGTH_SHORT)
                .show()
        }else if (businessEmail.isEmpty()){
            Toast.makeText(this, "Enter Email.", Toast.LENGTH_SHORT)
                .show()
        }else if (mobile.isEmpty()){
            Toast.makeText(this, "Enter business number.", Toast.LENGTH_SHORT)
                .show()
        }else{
            progressDialog.show("Saving Business Details...")
            if (imageUri != null){
                saveImage(businessName, businessEmail, businessWebsite, businessBio, mobile,
                    businessId.toString())
            }else saveBusinessDetailsWithoutLogo( businessName, businessEmail, businessWebsite, businessBio, mobile, businessId.toString())
        }

    }

    private fun saveImage(businessName: String, businessEmail: String, businessWebsite: String, businessBio: String, mobile: String, business_id: String) {
        val imageRef = constants.storageRef.child("business_logo/${business_id}.jpg")

        val uploadTask = imageRef.putFile(imageUri!!)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                imageRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                    val downloadUri = uri?.toString()
                    saveBusinessDetailsWithLogo(downloadUri!!, businessName, businessEmail, businessWebsite, businessBio, mobile, business_id)
                }
            } else {
                progressDialog.hide()
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBusinessDetailsWithoutLogo(businessName: String, businessEmail: String, businessWebsite: String, businessBio: String, mobile: String, business_id: String) {
        val businessDetails = hashMapOf(
            "business_name" to businessName,
            "business_email" to businessEmail,
            "business_website" to businessWebsite,
            "business_bio" to businessBio,
            "business_mobile" to mobile,
            "business_id" to business_id
        )

        constants.db.collection("businesses").document(business_id)
            .set(businessDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                Toast.makeText(this, "Changes save successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Toast.makeText(this, "Failed to save Changed: $e.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    private fun saveBusinessDetailsWithLogo(logoUrl: String, businessName: String, businessEmail: String, businessWebsite: String, businessBio: String, mobile: String, business_id: String) {
        val businessDetails = hashMapOf(
            "business_name" to businessName,
            "business_email" to businessEmail,
            "business_website" to businessWebsite,
            "business_bio" to businessBio,
            "business_mobile" to mobile,
            "business_logo" to logoUrl,
            "business_id" to business_id
        )

        constants.db.collection("businesses").document(business_id)
            .set(businessDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                Toast.makeText(this, "Changes save successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Toast.makeText(this, "Failed to save Changed: $e.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}