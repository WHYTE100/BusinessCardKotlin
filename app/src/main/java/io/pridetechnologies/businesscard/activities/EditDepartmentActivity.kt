package io.pridetechnologies.businesscard.activities

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.firestore.SetOptions
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.ActivityAddDepartmentBinding
import io.pridetechnologies.businesscard.databinding.ActivityEditDepartmentBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding

class EditDepartmentActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityEditDepartmentBinding
    private val constants = Constants()
    private var businessId: String? = ""
    private var departmentId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDepartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        businessId = intent.getStringExtra("business_id")
        departmentId = intent.getStringExtra("department_id")
        binding.saveButton.setOnClickListener {
            saveDepartmentContacts()
        }
        constants.db.collection("businesses").document(businessId.toString())
            .collection("departments").document(departmentId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val departmentName = snapshot.get("department_name").toString()
                    val departmentDesc = snapshot.get("department_desc").toString()
                    val departmentEmail = snapshot.get("department_email").toString()
                    val departmentWhatsApp = snapshot.get("department_whatsapp").toString()
                    val departmentMobile = snapshot.get("department_mobile").toString()

                    binding.departmentNameTextField.editText?.setText(departmentName)
                    binding.departmentDescTextField.editText?.setText(departmentDesc)
                    binding.departmentEmailTextField.editText?.setText(departmentEmail)
                    binding.departmentWhatsAppTextField.editText?.setText(departmentWhatsApp)
                    binding.departmentMobileTextField.editText?.setText(departmentMobile)

                } else {
                    Log.d(ContentValues.TAG, "Current data: null")
                }
            }
    }

    private fun saveDepartmentContacts() {
        val departmentName = binding.departmentNameTextField.editText?.text.toString().trim()
        val departmentDesc = binding.departmentDescTextField.editText?.text.toString().trim()
        val departmentEmail = binding.departmentEmailTextField.editText?.text.toString().trim()
        val departmentWhatsApp = binding.departmentWhatsAppTextField.editText?.text.toString().trim()
        val departmentMobile = binding.departmentMobileTextField.editText?.text.toString().trim()

        //Log.d(ContentValues.TAG, "User: $email \n $password")

        if (departmentName.isEmpty()){
            Toast.makeText(this, "Enter Department Name.", Toast.LENGTH_SHORT)
                .show()
        }else if (departmentDesc.isEmpty()){
            Toast.makeText(this, "Enter Department Description.", Toast.LENGTH_SHORT)
                .show()
        }else if (departmentMobile.isEmpty()){
            Toast.makeText(this, "Enter Department Number.", Toast.LENGTH_SHORT)
                .show()
        }else{

            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Save Details"
            b.descTextView.text = "Are you sure you want to save?"
            b.positiveTextView.text = "Save"
            b.positiveTextView.setOnClickListener {
                progressDialog.show("Saving Details...")
                val departmentDetails = hashMapOf(
                    "department_name" to departmentName,
                    "department_desc" to departmentDesc,
                    "department_email" to departmentEmail,
                    "department_mobile" to departmentMobile,
                    "department_whatsapp" to departmentWhatsApp,
                    "department_id" to departmentId
                )
                constants.db.collection("businesses").document(businessId.toString())
                    .collection("departments").document(departmentId.toString())
                    .set(departmentDetails, SetOptions.merge())
                    .addOnSuccessListener {
                        progressDialog.hide()
                        finish()
                        Animatoo.animateFade(this)
                        Toast.makeText(this, "Department saved successfully.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Toast.makeText(this, "Failed to save department: ${e}.", Toast.LENGTH_SHORT)
                        .show() }
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }
}