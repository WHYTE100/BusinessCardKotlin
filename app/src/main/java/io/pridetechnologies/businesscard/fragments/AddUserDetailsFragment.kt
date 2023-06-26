package io.pridetechnologies.businesscard.fragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentAddUserDetailsBinding
import java.io.ByteArrayOutputStream

class AddUserDetailsFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentAddUserDetailsBinding
    private val constants = Constants()
    private var bitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddUserDetailsBinding.inflate(layoutInflater, container, false)

        binding.nextButton.setOnClickListener {
            saveUserDetails()
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
            Toast.makeText(requireContext(), "Enter first name.", Toast.LENGTH_SHORT)
                .show()
        }else if (surname.isEmpty()){
            Toast.makeText(requireContext(), "Enter Surname.", Toast.LENGTH_SHORT)
                .show()
        }else if (profession.isEmpty()){
            Toast.makeText(requireContext(), "Enter profession.", Toast.LENGTH_SHORT)
                .show()
        }else if (mobile.isEmpty()){
            Toast.makeText(requireContext(), "Enter mobile.", Toast.LENGTH_SHORT)
                .show()
        }else{
            progressDialog.show("Saving Details...")
            updateUI(firstName, otherName, surname, profession, mobile)
        }
    }

    private fun updateUI(firstName: String, otherName: String, surname: String, profession: String, mobile: String) {

        val userDetails = hashMapOf(
            "first_name" to firstName,
            "surname" to surname,
            "other_names" to otherName,
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
                val action = AddUserDetailsFragmentDirections.actionAddUserDetailsFragmentToAddProfileImageFragment()
                findNavController().navigate(action)
                Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }


    }
}