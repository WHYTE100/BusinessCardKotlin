package io.pridetechnologies.businesscard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentResetPasswordBinding
class ResetPasswordFragment : Fragment() {
    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentResetPasswordBinding
    private val constants = Constants()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentResetPasswordBinding.inflate(layoutInflater, container, false)

        binding.button31.setOnClickListener {
            val email = binding.emailTextField.editText?.text.toString().trim()
            if (email.isEmpty()){
                constants.showToast(requireContext(), "Enter Email.")
            }else{

                progressDialog.show("Sending Reset Email...")
            constants.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        progressDialog.hide()
                        constants.showToast(requireContext(), "Email Sent.")
                    } else {
                        progressDialog.hide()
                        constants.showToast(requireContext(), "Failed to send email.")

                    }                    }
                }
        }
        binding.button30.setOnClickListener {
            val action = ResetPasswordFragmentDirections.actionResetPasswordFragmentToLoginFragment()
            findNavController().navigate(action)
        }
        return binding.root
    }

}