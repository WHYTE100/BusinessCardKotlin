package io.pridetechnologies.businesscard.fragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentAddUserDetailsBinding
import java.io.ByteArrayOutputStream

class AddUserDetailsFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentAddUserDetailsBinding
    private val constants = Constants()
    private var shortLink:Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddUserDetailsBinding.inflate(layoutInflater, container, false)

        binding.nextButton.setOnClickListener {
            saveUserDetails()
        }
        val deepLink = Uri.parse("https://businesscardmw.page.link/individuals")
            .buildUpon()
            .appendQueryParameter("key", constants.currentUserId.toString())
            .build()
        val dynamicLink = Firebase.dynamicLinks.dynamicLink {
            link = deepLink
            domainUriPrefix = "https://businesscardmw.page.link"
            androidParameters {}
        }
        val shortLinkTask = Firebase.dynamicLinks
            .shortLinkAsync {
                longLink = dynamicLink.uri
                domainUriPrefix = "https://businesscardmw.page.link"
                androidParameters {}
            }
        shortLinkTask.addOnSuccessListener { result ->
            shortLink = result.shortLink
        }.addOnFailureListener { exception ->
            Log.d(ContentValues.TAG, "Error creating short url", exception)
        }
        binding.mobile.setText(binding.ccp3.defaultCountryCodeWithPlus.toString())
        // Set a click listener for the CountryCodePicker
        binding.ccp3.setOnClickListener {
            // Show the country picker dialog
            binding.ccp3.launchCountrySelectionDialog()
        }

        // Set a listener for country selection
        binding.ccp3.setOnCountryChangeListener {
            // Get the selected country code with a plus sign (e.g., "+1")
            val selectedCountryCodeWithPlus = binding.ccp3.selectedCountryCodeWithPlus
            // Display the selected country code in the EditText
            binding.mobile.setText(selectedCountryCodeWithPlus)
        }

        return binding.root
    }

    private fun saveUserDetails() {

        val firstName = binding.firstName.text.toString().trim()
        val otherName = binding.otherName.text.toString().trim()
        val surname = binding.surname.text.toString().trim()
        val profession = binding.profession.text.toString().trim()
        val mobile = binding.mobile.text.toString().trim()

        //Log.d(ContentValues.TAG, "User: $email \n $password")

        if (firstName.isEmpty()){
            constants.showToast(requireContext(), "Enter first name.")
        }else if (surname.isEmpty()){
            constants.showToast(requireContext(), "Enter Surname.")
        }else if (profession.isEmpty()){
            constants.showToast(requireContext(), "Enter profession.")
        }else if (mobile.isEmpty()){
            constants.showToast(requireContext(), "Enter mobile.")
        }else{
            progressDialog.show("Saving Details...")
            updateUI(firstName, otherName, surname, profession, mobile)
        }
    }

    private fun updateUI(firstName: String, otherName: String, surname: String, profession: String, mobile: String) {

        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = multiFormatWriter.encode(constants.currentUserId.toString(), BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            val bytes = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            val data = bytes.toByteArray()
            val encodedCode = Base64.encodeToString(data, Base64.DEFAULT)
            constants.writeToSharedPreferences(requireContext(),"user_qr_code", encodedCode)

            val qrcodeRef = constants.storageRef.child("user_qr_code/${constants.currentUserId.toString()}.jpg")
            val uploadTask = qrcodeRef.putBytes(data)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the download URL of the uploaded image
                    qrcodeRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                        val downloadUrl = uri?.toString()
                        val userId = constants.currentUserId.toString()
                        val userEmail = constants.currentUser?.email.toString()
                        val userDetails = hashMapOf(
                            "profile_image_url" to null,
                                    "first_name" to firstName,
                            "surname" to surname,
                            "other_names" to otherName,
                            "profession" to profession,
                            "mobile" to mobile,
                            "user_qr_code" to downloadUrl,
                            "email" to userEmail,
                            "user_bio" to "",
                            "encode_qr_code" to encodedCode,
                            "user_link" to shortLink.toString(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "uid" to userId
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
                                val action = AddUserDetailsFragmentDirections.actionAddUserDetailsFragmentToAddProfileImageFragment()
                                findNavController().navigate(action)
                            }
                            .addOnFailureListener { e ->
                                progressDialog.hide()
                                constants.showToast(requireContext(), e.message.toString())
                            }

                    }.addOnFailureListener {
                        // Handle any error that occurs while getting the download URL
                        Log.d(ContentValues.TAG, "Failed to get the url")
                    }
                } else {
                    // Handle any error that occurs during upload
                    Log.d(ContentValues.TAG, "Failed to upload the code")
                }
            }
        } catch (_: Exception) {
        }



    }

}