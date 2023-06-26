package io.pridetechnologies.businesscard.fragments

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentLoginBinding
    private val constants = Constants()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)


        val auth = FirebaseAuth.getInstance()
        //val currentUserId = auth.currentUser?.uid

        binding.loginButton.setOnClickListener {
            loginUser(auth)
        }
        binding.textView5.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
        //progressDialog.show("Signing In...")

        return binding.root
    }

    private fun loginUser(auth: FirebaseAuth) {
        val email = binding.emailTextField.editText?.text.toString().trim()
        val password = binding.passwordTextField.editText?.text.toString().trim()

        if (email.isEmpty()){
            Toast.makeText(requireContext(), "Enter Email.", Toast.LENGTH_SHORT)
                .show()
        }else if (password.isEmpty()){
            Toast.makeText(requireContext(), "Enter Password.", Toast.LENGTH_SHORT)
                .show()
        }else{
            progressDialog.show("Signing In...")
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {

                val userId = Firebase.auth.currentUser?.uid
                constants.db.collection("users").document(userId.toString())
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null){
                            val firstName = document.get("first_name").toString()
                            val surname = document.get("surname").toString()
                            val profileImage = document.get("profile_image_url").toString()
                            val bio = document.get("user_bio").toString()

                            val encodedCode = document.get("encode_qr_code").toString()
                            constants.writeToSharedPreferences(requireContext(),"user_qr_code", encodedCode)
                            if (firstName.isEmpty() && surname.isEmpty()){
                                progressDialog.hide()
                                val action = LoginFragmentDirections.actionLoginFragmentToAddUserDetailsFragment()
                                findNavController().navigate(action)
                            }else if (profileImage.isEmpty()){
                                progressDialog.hide()
                                val action = LoginFragmentDirections.actionLoginFragmentToAddProfileImageFragment()
                                findNavController().navigate(action)
                            }else if (bio.isEmpty()){
                                progressDialog.hide()
                                val action = LoginFragmentDirections.actionLoginFragmentToAddBioFragment()
                                findNavController().navigate(action)
                            }else {
                                progressDialog.hide()
                                val action = LoginFragmentDirections.actionLoginFragmentToHomeActivity()
                                findNavController().navigate(action)
                            }
                        }else{
                            progressDialog.hide()
                            val action = LoginFragmentDirections.actionLoginFragmentToHomeActivity()
                            findNavController().navigate(action)
                        }
                    }
                    .addOnFailureListener { exception ->
                        progressDialog.hide()
                        val action = LoginFragmentDirections.actionLoginFragmentToHomeActivity()
                        findNavController().navigate(action)
                        //constants.setSnackBar(requireContext(), exception.toString())
                        //Log.w(ContentValues.TAG, "Error getting documents: ", exception)
                    }

            }.addOnFailureListener { exception ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
        }
    }
}