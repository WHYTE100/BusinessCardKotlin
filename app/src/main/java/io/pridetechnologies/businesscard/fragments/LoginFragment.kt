package io.pridetechnologies.businesscard.fragments

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentLoginBinding
    private val constants = Constants()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private val googleSignInResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
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
                                // Check condition
                                if (task.isSuccessful) {
                                    val userId = Firebase.auth.currentUser?.uid
                                   sendToHome(userId)
                                } else {
                                    // When task is unsuccessful display Toast
                                    constants.showToast(requireContext(),
                                        "Authentication Failed : ${task.exception?.message}"
                                    )
                                }
                            }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)


        val auth = FirebaseAuth.getInstance()
        //val currentUserId = auth.currentUser?.uid
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("155854465270-kl1tc1fetqu3knvnrahddluqg988ocoe.apps.googleusercontent.com")
            .requestEmail()
            .build()

        // Initialize sign in client
        googleSignInClient = GoogleSignIn.getClient(requireContext(), googleSignInOptions)
        binding.googleSignInButton.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            // Start activity for result
            googleSignInResultContract.launch(intent)
        }

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
                sendToHome(userId)

            }.addOnFailureListener { exception ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
        }
    }

    private fun sendToHome(userId: String?) {

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
                    val action = LoginFragmentDirections.actionLoginFragmentToAddUserDetailsFragment()
                    findNavController().navigate(action)
                }
            }
            .addOnFailureListener {
                progressDialog.hide()
                val action = LoginFragmentDirections.actionLoginFragmentToHomeActivity()
                findNavController().navigate(action)
                //constants.setSnackBar(requireContext(), exception.toString())
                //Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }
}