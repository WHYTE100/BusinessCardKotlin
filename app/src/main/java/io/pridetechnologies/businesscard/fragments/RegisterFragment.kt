package io.pridetechnologies.businesscard.fragments

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.MainActivity
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.FragmentRegisterBinding
import java.io.ByteArrayOutputStream

class RegisterFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentRegisterBinding
    private val constants = Constants()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)

        val auth = FirebaseAuth.getInstance()
        binding.registerButton.setOnClickListener {
            registerUser(auth)
        }
        binding.textView5.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        return binding.root
    }
    private fun registerUser(auth: FirebaseAuth) {

        val email = binding.emailTextField.editText?.text.toString().trim()
        val password = binding.passwordTextField.editText?.text.toString().trim()
        val confirmPassword = binding.password2TextField.editText?.text.toString().trim()

        //Log.d(ContentValues.TAG, "User: $email \n $password")

        if (email.isEmpty()){
            Toast.makeText(requireContext(), "Enter Email.", Toast.LENGTH_SHORT)
                .show()
        }else if (password.isEmpty()){
            Toast.makeText(requireContext(), "Enter Password.", Toast.LENGTH_SHORT)
                .show()
        }else if (confirmPassword.isEmpty()){
            Toast.makeText(requireContext(), "Enter Confirmation Password.", Toast.LENGTH_SHORT)
                .show()
        }else{
            progressDialog.show("Signing Up...")
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                val user = constants.auth.currentUser
                user?.let {
                    it.updateEmail(email)
                    it.sendEmailVerification()
                        .addOnSuccessListener {
                            progressDialog.hide()
                            constants.auth.signOut()
                            val dialog = Dialog(requireContext())
                            val b = CustomDialogBoxBinding.inflate(layoutInflater)
                            dialog.setContentView(b.root)
                            b.titleTextView.text = "Verify your email"
                            b.descTextView.text = "A verification link has been sent to your email. Please open your email to verify your account and then login"
                            b.positiveTextView.text = "Ok"
                            b.positiveTextView.setOnClickListener {
                                val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
                                findNavController().navigate(action)
                            }
                            b.negativeTextView.visibility = View.GONE
                            dialog.setCancelable(false)
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.show()

                        }.addOnFailureListener {
                            progressDialog.hide()
                            constants.showToast(requireContext(), "Error sending verification email ")
                        }
                }

            }.addOnFailureListener {
                progressDialog.hide()
                constants.showToast(requireContext(), it.message.toString())
            }
        }
    }
}