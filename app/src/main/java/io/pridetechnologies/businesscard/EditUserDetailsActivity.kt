package io.pridetechnologies.businesscard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.databinding.ActivityEditUserDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class EditUserDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditUserDetailsBinding
    private val constants = Constants()
    private val progressDialog by lazy { CustomProgressDialog(this) }

    private lateinit var imageUri: Uri

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            binding.userImageView.setImageURI(uri)
            if (uri != null) {
                imageUri = uri
                saveImage()
            }
        }
    }
    private val getCameraImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            val uri = getImageUri(imageBitmap)
            binding.userImageView.setImageURI(uri)
            imageUri = uri
            saveImage()
        }
    }
    private fun getImageUri(inImage: Bitmap): Uri {

        val tempFile = File.createTempFile("image", ".png")
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fileOutPut = FileOutputStream(tempFile)
        fileOutPut.write(bitmapData)
        fileOutPut.flush()
        fileOutPut.close()
        return Uri.fromFile(tempFile)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        constants.db.collection("users").document(constants.currentUserId.toString())
            .addSnapshotListener { value, _ ->
                val userBio = value?.get("user_bio").toString()
                val userImage = value?.get("profile_image_url").toString()
                val firstName = value?.get("first_name").toString()
                val surname = value?.get("surname").toString()
                val mobile = value?.get("mobile").toString()
                val userProfession = value?.get("profession").toString()

                if (!userImage.equals(null)){
                    Picasso.get().load(userImage).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.userImageView)
                }

                binding.firstName.setText(firstName)
                binding.surname.setText(surname)
                binding.mobile.setText(mobile)
                binding.profession.setText(userProfession)
                binding.bio.setText(userBio)



            }

        binding.nextButton.setOnClickListener {
            saveUserDetails()
        }
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.changeImageButton.setOnClickListener {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                selectImage()
//                                val options: Array<String> = arrayOf("Camera","Gallery","Cancel")
//                                val builder= AlertDialog.Builder(this@EditUserDetailsActivity)
//                                builder.setTitle("Add Image").setItems(options) { dialog, position ->
//                                    when (position) {
//                                        0 -> {
//                                            openCamera()
//                                        }
//                                        1 -> {
//                                            selectImage()
//                                        }
//                                        else -> {
//                                            dialog.dismiss()
//                                        }
//                                    }
//                                }.show()
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

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        getCameraImage.launch(takePictureIntent)
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        getGalleryImage.launch(intent)
    }

    private fun saveImage() {
        progressDialog.show("Saving Image...")

        val userId = constants.auth.currentUser?.uid.toString()
        val imageRef = constants.storageRef.child("user_profile_image/${userId}.jpg")

        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                imageRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                    val downloadUri = uri?.toString()
                    val userDetails = hashMapOf(
                        "profile_image_url" to downloadUri.toString()
                    )
                    val user = constants.auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        photoUri = Uri.parse(downloadUri)
                    }
                    user!!.updateProfile(profileUpdates)

                    constants.db.collection("users").document(userId)
                        .set(userDetails, SetOptions.merge())
                        .addOnSuccessListener {
                            progressDialog.hide()
                            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.hide()
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            //Log.w(ContentValues.TAG, "Error writing document", e)
                        }
                }


            } else {
                progressDialog.hide()
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveUserDetails() {

        val firstName = binding.firstName.text.toString().trim()
        val otherName = binding.otherName.text.toString().trim()
        val surname = binding.surname.text.toString().trim()
        val profession = binding.profession.text.toString().trim()
        val bio = binding.bio.text.toString().trim()
        val mobile = binding.mobile.text.toString().trim()

        //Log.d(ContentValues.TAG, "User: $email \n $password")

        if (firstName.isEmpty()){
            Toast.makeText(this, "Enter first name.", Toast.LENGTH_SHORT)
                .show()
        }else if (surname.isEmpty()){
            Toast.makeText(this, "Enter Surname.", Toast.LENGTH_SHORT)
                .show()
        }else if (profession.isEmpty()){
            Toast.makeText(this, "Enter profession.", Toast.LENGTH_SHORT)
                .show()
        }else if (mobile.isEmpty()){
            Toast.makeText(this, "Enter mobile.", Toast.LENGTH_SHORT)
                .show()
        }else if (mobile.isNotEmpty() && !constants.isValidPhoneNumber(mobile)){
            constants.showToast(this, "Your Mobile number should be in this format +1XXXXXXXXXXX.")
        }else{
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Save Details"
            b.descTextView.text = "Are you sure you want to save?"
            b.positiveTextView.text = "Save"
            b.positiveTextView.setOnClickListener {
                progressDialog.show("Saving Details...")
                updateUI(firstName, otherName, surname, profession, mobile, bio)
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }

    private fun updateUI(
        firstName: String,
        otherName: String,
        surname: String,
        profession: String,
        mobile: String,
        bio: String
    ) {

        val userDetails = hashMapOf(
            "first_name" to firstName,
            "surname" to surname,
            "other_names" to otherName,
            "user_bio" to bio,
            "profession" to profession,
            "mobile" to mobile
        )
        val user = constants.auth.currentUser
        val profileUpdates = userProfileChangeRequest {
            displayName = "$firstName $surname"
        }
        user!!.updateProfile(profileUpdates)

        constants.db.collection("users").document(constants.currentUserId.toString())
            .set(userDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                Toast.makeText(this, "Changes Saved.", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Toast.makeText(this, "Failed to save Changed: $e.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}