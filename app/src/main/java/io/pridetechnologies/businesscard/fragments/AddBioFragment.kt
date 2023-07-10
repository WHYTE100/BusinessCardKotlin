package io.pridetechnologies.businesscard.fragments

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.FragmentAddBioBinding
import io.pridetechnologies.businesscard.databinding.FragmentAddProfileImageBinding
import java.lang.Exception


class AddBioFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentAddBioBinding
    private val constants = Constants()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddBioBinding.inflate(layoutInflater, container, false)

        binding.button31.setOnClickListener {
            saveUserDetails()
        }
        binding.button30.setOnClickListener {
            val action = AddBioFragmentDirections.actionAddBioFragmentToHomeActivity()
            findNavController().navigate(action)
        }
        return binding.root
    }

    private fun saveUserDetails() {
        val bio = binding.userBio1.text.toString().trim()

        if (bio.isEmpty()){
            constants.showToast(requireContext(), "Enter bio.")
        }else{
            progressDialog.show("Saving bio...")
            updateUI(bio)
        }
    }

    private fun updateUI(bio: String) {

        val userDetails = hashMapOf(
            "user_bio" to bio
        )

        constants.db.collection("users").document(constants.currentUserId.toString())
            .set(userDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                val action = AddBioFragmentDirections.actionAddBioFragmentToHomeActivity()
                findNavController().navigate(action)
            }
            .addOnFailureListener { e: Exception ->
                progressDialog.hide()
                constants.showToast(requireContext(), e.message.toString())}


    }

}