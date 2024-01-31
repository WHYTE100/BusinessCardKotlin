package io.pridetechnologies.businesscard.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentLoginBinding
    private val constants = Constants()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private val googleSignInResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.e(ContentValues.TAG, "data1: ${result.data?.clipData}")
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
//            Log.e(ContentValues.TAG, "data2: ${data.toString()}")
//            Log.e(ContentValues.TAG, result.resultCode.toString())
            val signInAccountTask: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            // check condition
            if (signInAccountTask.isSuccessful) {
                // Initialize sign in account
                try {
                    // Initialize sign in account
                    val googleSignInAccount = signInAccountTask.getResult(ApiException::class.java)
                    // Check condition
                    if (googleSignInAccount != null) {
                        // When sign in account is not equal to null initialize auth credential
                        val authCredential: AuthCredential = GoogleAuthProvider.getCredential(
                            googleSignInAccount.idToken, null
                        )
                        // Check credential
                        auth.signInWithCredential(authCredential)
                            .addOnCompleteListener{ task ->
                                val user = Firebase.auth.currentUser
                                if (task.isSuccessful) {
                                    if (user?.isEmailVerified == true){
                                        val userId = user.uid
                                        sendToHome(userId)
                                    }else{
                                        progressDialog.hide()
                                        user?.sendEmailVerification()
                                        auth.signOut()
                                        constants.showToast(requireContext(), "Please verify your email address first")
                                    }
                                } else {
                                    // When task is unsuccessful display Toast
                                    constants.showToast(requireContext(),
                                        "Authentication Failed : ${task.exception?.message}"
                                    )
                                }
                            }
                    }
                } catch (e: FirebaseAuthException) {
                    constants.showToast(requireContext(), e.message.toString())
                    Log.e(ContentValues.TAG, e.message.toString())
                }
            }else{
                Log.e(ContentValues.TAG, result.data.toString())
                constants.showToast(requireContext(), result.data.toString())
            }
        }else{
            constants.showToast(requireContext(), result.data.toString())
            Log.e(ContentValues.TAG, result.resultCode.toString())
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)


        //val auth = FirebaseAuth.getInstance()

//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//
//        // Initialize sign in client
//        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
//        binding.googleSignInButton.setOnClickListener {
//            val intent = googleSignInClient.signInIntent
//            // Start activity for result
//            googleSignInResultContract.launch(intent)
//        }

        binding.loginButton.setOnClickListener {
            loginUser()
        }
        binding.passwordResetButton.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToResetPasswordFragment()
            findNavController().navigate(action)
        }
        binding.textView5.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
        return binding.root
    }

    private fun loginUser() {
        val email = binding.emailTextField.editText?.text.toString().trim()
        val password = binding.passwordTextField.editText?.text.toString().trim()

        if (email.isEmpty()){
            constants.showToast(requireContext(), "Enter Email.")
        }else if (password.isEmpty()){
            constants.showToast(requireContext(), "Enter Password.")
        }else{

            progressDialog.show("Signing In...")
            constants.auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                if (it.user?.isEmailVerified == true){
                    val userId = it.user?.uid
                    sendToHome(userId)
                }else{
                    progressDialog.hide()
                    it.user?.sendEmailVerification()
                    constants.auth.signOut()
                    constants.showToast(requireContext(), "Please verify your email address first")
                }

            }.addOnFailureListener { exception ->
                progressDialog.hide()
                constants.showToast(requireContext(), "Error: ${exception.message.toString()}")
            }
        }
    }

    private fun sendToHome(userId: String?) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (!it.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = it.result
            val map = mapOf(
                "token" to token.toString()
            )
            constants.db.collection("users").document(userId.toString()).set(map, SetOptions.merge())

        }
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
                        val action = LoginFragmentDirections.actionLoginFragmentToPermissionsFragment()
                        findNavController().navigate(action)
                    }else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        progressDialog.hide()
                        val action = LoginFragmentDirections.actionLoginFragmentToPermissionsFragment()
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
                    val action = LoginFragmentDirections.actionLoginFragmentToPermissionsFragment()
                    findNavController().navigate(action)
                }
            }
            .addOnFailureListener {
                progressDialog.hide()
                val action = LoginFragmentDirections.actionLoginFragmentToHomeActivity()
                findNavController().navigate(action)
            }
    }
}