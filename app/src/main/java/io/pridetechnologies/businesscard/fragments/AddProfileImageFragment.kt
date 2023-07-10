package io.pridetechnologies.businesscard.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.FragmentAddProfileImageBinding
import io.pridetechnologies.businesscard.databinding.FragmentAddUserDetailsBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AddProfileImageFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentAddProfileImageBinding
    private val constants = Constants()

    private lateinit var imageUri: Uri

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            binding.imageView5.setImageURI(uri)
            if (uri != null) {
                imageUri = uri
                binding.button7.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddProfileImageBinding.inflate(layoutInflater, container, false)

        binding.button7.setOnClickListener { saveImage() }

//        binding.skipButton.setOnClickListener {
//            val action = AddProfileImageFragmentDirections.actionAddProfileImageFragmentToAddBioFragment()
//            findNavController().navigate(action)
//        }

        binding.button5.setOnClickListener {

            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                val intent = Intent()
                                intent.type = "image/*"
                                intent.action = Intent.ACTION_GET_CONTENT
                                getGalleryImage.launch(intent)
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
        return binding.root
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
                            val action = AddProfileImageFragmentDirections.actionAddProfileImageFragmentToAddBioFragment()
                            findNavController().navigate(action)
                        }
                        .addOnFailureListener { e ->
                            progressDialog.hide()
                            constants.showToast(requireContext(), e.message.toString())
                        }
                }


            } else {
                progressDialog.hide()
                constants.showToast(requireContext(), "Failed to upload image")
            }
        }
    }
}
