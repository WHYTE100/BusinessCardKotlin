package io.pridetechnologies.businesscard.activities

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.crashlytics.ktx.crashlytics
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
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.ActivityNewBusinessBinding
import java.io.ByteArrayOutputStream

class NewBusinessActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBusinessBinding
    private val constants = Constants()
    private val progressDialog by lazy { CustomProgressDialog(this) }

    private var imageUri: Uri? = null

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            binding.userImageView.setImageURI(uri)
            if (uri != null) {
                imageUri = uri
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val myPosition = binding.myPosition.text.toString().trim()

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
            if (constants.hasInternetConnection(this)) {
                progressDialog.show("Saving Business Details...")
                val businessId = constants.db.collection("businesses").document().id
                if (imageUri != null){
                    saveImage(businessName, businessEmail, businessWebsite, businessBio, mobile, myPosition, businessId)
                }else saveBusinessDetails(
                    null,
                    businessName,
                    businessEmail,
                    businessWebsite,
                    businessBio,
                    mobile,
                    myPosition,
                    businessId
                )
            } else {
                constants.showToast(this, "No Internet Connection")
            }
        }

    }

    private fun saveImage(
        businessName: String,
        businessEmail: String,
        businessWebsite: String,
        businessBio: String,
        mobile: String,
        myPosition: String,
        businessId: String
    ) {
        val imageRef = constants.storageRef.child("business_logo/${businessId}.jpg")

        val uploadTask = imageRef.putFile(imageUri!!)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                imageRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                    val downloadUri = uri?.toString()
                    saveBusinessDetails(downloadUri!!, businessName, businessEmail, businessWebsite, businessBio, mobile, myPosition, businessId)
                }


            } else {
                progressDialog.hide()
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBusinessDetails(
        logoUrl: String?,
        businessName: String,
        businessEmail: String,
        businessWebsite: String,
        businessBio: String,
        mobile: String,
        myPosition: String,
        businessId: String
    ) {
        val shortLink = constants.createBusinessesDynamicLink(businessId, businessName)
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = multiFormatWriter.encode(businessId, BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            val bytes = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            val data = bytes.toByteArray()

            val qrcodeRef = constants.storageRef.child("business_qr_code/${businessId}.jpg")
            val uploadTask = qrcodeRef.putBytes(data)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the download URL of the uploaded image
                    qrcodeRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                        val qrCodeDownloadUrl = uri?.toString()
                        updateUI(logoUrl!!, businessName, businessEmail, businessWebsite, businessBio, mobile, myPosition, businessId, qrCodeDownloadUrl, shortLink)

                    }.addOnFailureListener {
                        /Firebase.crashlytics.recordException(it)
                    }
                } else {
                    // Handle any error that occurs during upload
                    Log.d(ContentValues.TAG, "Failed to upload the code")
                }
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
        }
    }

    private fun updateUI(

        logoUrl: String,
        businessName: String,
        businessEmail: String,
        businessWebsite: String,
        businessBio: String,
        mobile: String,
        myPosition: String,
        businessId: String,
        qrCodeDownloadUrl: String?,
        shortLink: Uri?
    ) {

        val businessDetails = hashMapOf(
            "business_name" to businessName,
            "business_email" to businessEmail,
            "business_website" to businessWebsite,
            "business_bio" to businessBio,
            "business_mobile" to mobile,
            "business_logo" to logoUrl,
            "business_qr_code" to qrCodeDownloadUrl,
            "business_link" to shortLink,
            "create_on" to FieldValue.serverTimestamp(),
            "business_id" to businessId
        )
        val user = constants.auth.currentUser
        var photoUrl: String? = null
        var userName: String? = null
        user?.let {
             photoUrl = it.photoUrl.toString()
             userName = it.displayName
        }
        val teamMemberDetails = hashMapOf(
            "user_name" to userName,
            "user_image" to photoUrl,
            "admin" to true,
            "super_admin" to true,
            "is_accepted" to true,
            "user_position" to myPosition,
            "added_on" to FieldValue.serverTimestamp(),
            "member_id" to constants.currentUserId.toString()
        )
        val myBusinessDetails = hashMapOf(
            "business_name" to businessName,
            "business_logo" to logoUrl,
            "user_position" to myPosition,
            "admin" to true,
            "is_accepted" to true,
            "added_on" to FieldValue.serverTimestamp(),
            "business_id" to businessId
        )
        constants.db.collection("users").document(constants.currentUserId.toString()).collection("my_work_place").document(businessId).set(myBusinessDetails, SetOptions.merge())
        constants.db.collection("businesses").document(businessId).collection("team").document(constants.currentUserId.toString()).set(teamMemberDetails, SetOptions.merge())

        constants.db.collection("businesses").document(businessId)
            .set(businessDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                val intent = Intent(this, BusinessAddressActivity::class.java)
                intent.putExtra("business_id", businessId)
                startActivity(intent)
                finish()
                Animatoo.animateFade(this)
            }
            .addOnFailureListener { e ->
                Firebase.crashlytics.recordException(e)
                progressDialog.hide()
                Toast.makeText(this, "Failed to save Changed: $e.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}