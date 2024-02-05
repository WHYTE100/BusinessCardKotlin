package io.pridetechnologies.businesscard.fragments

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.databinding.FragmentPermissionsBinding

class PermissionsFragment : Fragment() {

    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentPermissionsBinding
    private val constants = Constants()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPermissionsBinding.inflate(layoutInflater, container, false)

        val storageCheckBox = binding.storageCheckBox
        val cameraCheckBox = binding.cameraCheckBox
        val audioCheckBox = binding.audioCheckBox
        val phoneCheckBox = binding.phoneCheckBox
        val locationCheckBox = binding.locationCheckBox

        storageCheckBox.setOnClickListener{
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                storageCheckBox.isChecked = true
                            }else{
                                storageCheckBox.isChecked = false
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

        cameraCheckBox.setOnClickListener{
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.CAMERA
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                cameraCheckBox.isChecked = true
                            }else{
                                cameraCheckBox.isChecked = false
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
        audioCheckBox.setOnClickListener{
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.RECORD_AUDIO
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                audioCheckBox.isChecked = true
                            }else{
                                audioCheckBox.isChecked = false
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
        phoneCheckBox.setOnClickListener{
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.CALL_PHONE
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                phoneCheckBox.isChecked = true
                            }else{
                                phoneCheckBox.isChecked = false
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
        locationCheckBox.setOnClickListener{
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()){
                                locationCheckBox.isChecked = true
                            }else{
                                locationCheckBox.isChecked = false
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

        binding.nextButton.setOnClickListener{
            progressDialog.show("Please wait...")
            if (storageCheckBox.isChecked && cameraCheckBox.isChecked && phoneCheckBox.isChecked && audioCheckBox.isChecked && locationCheckBox.isChecked){
                constants.db.collection("users").document(constants.currentUserId.toString())
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null){
                            val firstName = document.get("first_name").toString()
                            val surname = document.get("surname").toString()

                            if (firstName.isEmpty() && surname.isEmpty()){
                                progressDialog.hide()
                                val action = PermissionsFragmentDirections.actionPermissionsFragmentToAddUserDetailsFragment()
                                findNavController().navigate(action)
                            }else {
                                progressDialog.hide()
                                val action = PermissionsFragmentDirections.actionPermissionsFragmentToHomeActivity()
                                findNavController().navigate(action)
                            }
                        }else{
                            progressDialog.hide()
                            val action = PermissionsFragmentDirections.actionPermissionsFragmentToAddUserDetailsFragment()
                            findNavController().navigate(action)
                        }
                    }
                    .addOnFailureListener {
                        progressDialog.hide()
                        val action = PermissionsFragmentDirections.actionPermissionsFragmentToAddUserDetailsFragment()
                        findNavController().navigate(action)
                    }
            } else {
                constants.showToast(requireContext(), "Please approve all the permissions")
            }
        }

        return binding.root
    }

}