package io.pridetechnologies.businesscard

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.firestore.SetOptions
import io.pridetechnologies.businesscard.databinding.ActivityAddSocialMediaBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding

class AddSocialMediaActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityAddSocialMediaBinding
    private val constants = Constants()
    private var userId: String = ""
    private var message: String = ""
    private var linkValue: String = ""
    private var currentText: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSocialMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }

        userId = intent.getStringExtra("user_id").toString()
        message = intent.getStringExtra("message").toString()
        linkValue = intent.getStringExtra("link_value").toString()
        currentText = intent.getStringExtra("current_text").toString()
        binding.textView12.text = message
        binding.linkTextField.editText?.setText(currentText)


        binding.continueButton.setOnClickListener {
            saveLink()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun saveLink() {

        val link = binding.linkTextField.editText?.text.toString().trim()
        if (link.isEmpty()){
            Toast.makeText(this, "No link Found", Toast.LENGTH_SHORT).show()
        }else if (linkValue == "" && !constants.isValidPhoneNumber(link)){
            constants.showToast(this, "Your Mobile number should be in this format +1XXXXXXXXXXX.")
        }else{
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Save Link"
            b.descTextView.text = "Are you sure you want to save this link?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                progressDialog.show("Saving link...")

                val linkDetails = hashMapOf(
                    linkValue to link
                )
                constants.db.collection("social_media").document(userId)
                    .set(linkDetails, SetOptions.merge())
                    .addOnSuccessListener {
                        progressDialog.hide()
                        constants.showToast(this, "Link saved successfully ")
                        finish()
                    }.addOnFailureListener { e ->
                        progressDialog.hide()
                        constants.showToast(this, "Error saving link ")
                        Log.w(ContentValues.TAG, "Error writing document", e) }
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()

        }

    }
}