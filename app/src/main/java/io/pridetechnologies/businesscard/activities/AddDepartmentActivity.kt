package io.pridetechnologies.businesscard.activities

import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.ActivityAddDepartmentBinding
import io.pridetechnologies.businesscard.databinding.ActivityAdminBusinesProfileBinding
import io.pridetechnologies.businesscard.fragments.RegisterFragmentDirections

class AddDepartmentActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityAddDepartmentBinding
    private val constants = Constants()
    private var businessId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDepartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        businessId = intent.getStringExtra("business_id")
        binding.saveButton.setOnClickListener {
            saveDepartmentContacts()
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
            progressDialog.show("Saving Details...")
            val departmentId = constants.db.collection("businesses").document(businessId.toString()).collection("departments").document().id
            val departmentDetails = hashMapOf(
                "department_name" to departmentName,
                "department_desc" to departmentDesc,
                "department_email" to departmentEmail,
                "department_mobile" to departmentMobile,
                "department_whatsapp" to departmentWhatsApp,
                "department_id" to departmentId
            )
            constants.db.collection("businesses").document(businessId.toString())
                .collection("departments").document(departmentId)
                .set(departmentDetails, SetOptions.merge())
                .addOnSuccessListener {
                    progressDialog.hide()
                    finish()
                    Animatoo.animateFade(this)
                }
                .addOnFailureListener { e ->
                    progressDialog.hide()
                    Log.w(ContentValues.TAG, "Error writing document", e) }
        }
    }
}