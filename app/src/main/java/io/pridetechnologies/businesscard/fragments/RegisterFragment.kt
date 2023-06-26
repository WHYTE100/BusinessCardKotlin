package io.pridetechnologies.businesscard.fragments

import android.content.ContentValues
import android.graphics.Bitmap
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
//                user!!.sendEmailVerification()
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            //Log.d(TAG, "Email sent.")
//
//                        }
//                    }
                user!!.updateEmail(email)
                createDynamicLink(user)

            }.addOnFailureListener {
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error creating user", it)
            }
        }
    }

    private fun createDynamicLink(user: FirebaseUser?) {

        val deepLink = Uri.parse("https://businesscardmw.page.link/individuals")
            .buildUpon()
            .appendQueryParameter("key", user?.uid.toString())
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
            val shortLink = result.shortLink
            //val shortLinkUrl = result.previewLink
            saveDataAndUpdateUI(shortLink.toString(), user)
        }.addOnFailureListener { exception ->
            Log.d(ContentValues.TAG, "Error creating short url", exception)
        }
    }

    private fun saveDataAndUpdateUI(link: String, user: FirebaseUser?) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = multiFormatWriter.encode(user?.uid.toString(), BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            val bytes = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            val data = bytes.toByteArray()
            val encodedCode = Base64.encodeToString(data, Base64.DEFAULT)
            constants.writeToSharedPreferences(requireContext(),"user_qr_code", encodedCode)

            val qrcodeRef = constants.storageRef.child("user_qr_code/${user?.uid.toString()}.jpg")
            val uploadTask = qrcodeRef.putBytes(data)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the download URL of the uploaded image
                    qrcodeRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                        val downloadUrl = uri?.toString()
                        val userId = user?.uid.toString()
                        val userEmail = user?.email.toString()
                        val userDetails = hashMapOf(
                            "user_qr_code" to downloadUrl,
                            "email" to userEmail,
                            "encode_qr_code" to encodedCode,
                            "user_link" to link,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "uid" to userId
                        )
                        constants.db.collection("users").document(userId)
                            .set(userDetails, SetOptions.merge())
                            .addOnSuccessListener {
                                progressDialog.hide()
                                val action = RegisterFragmentDirections.actionRegisterFragmentToAddUserDetailsFragment()
                                findNavController().navigate(action)
                                Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
                            }
                            .addOnFailureListener { e ->
                                progressDialog.hide()
                                Log.w(ContentValues.TAG, "Error writing document", e) }

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