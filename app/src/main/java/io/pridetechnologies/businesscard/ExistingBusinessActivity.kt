package io.pridetechnologies.businesscard

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.databinding.ActivityExistingBusinessBinding
import io.pridetechnologies.businesscard.databinding.ActivityNewBusinessCardBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding

class ExistingBusinessActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityExistingBusinessBinding
    private val constants = Constants()
    private var businessId: String? = ""
    private var userName: String? = ""
    private var userImage: String? = ""
    private var businessLogo: String? = ""
    private var businessName: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExistingBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = constants.auth.currentUser
        user?.let {
            userImage = it.photoUrl.toString()
            userName = it.displayName
        }

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        binding.sendButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Link to business"
            b.descTextView.text = "Are you sure you want to link your profile to this business?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                sendLinkingRequest()
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
        businessId = intent.getStringExtra("business_id")
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { value, _ ->
                businessLogo = value?.get("business_logo").toString()
                businessName = value?.get("business_name").toString()
                val businessArea = value?.get("area_located").toString()
                val businessDistrict = value?.get("district_name").toString()
                val businessCountry = value?.get("country").toString()

                Picasso.get().load(businessLogo).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.businessLogoView)
                binding.businessLocationTextView.text = "${businessArea}, ${businessDistrict}, ${businessCountry}"
                binding.businessNameView.text = businessName
            }
    }

    private fun sendLinkingRequest() {
        val memberPosition = binding.positionTextField.editText?.text.toString().trim()

        if (memberPosition.isEmpty()){
            Toast.makeText(this, "Enter Your Position.", Toast.LENGTH_SHORT)
                .show()
        }else{
            progressDialog.show("Saving Details...")
            val teamMemberDetails = hashMapOf(
                "user_name" to userName,
                "user_image" to userImage,
                "admin" to false,
                "super_admin" to false,
                "is_accepted" to false,
                "user_position" to memberPosition,
                "approve_on" to FieldValue.serverTimestamp(),
                "business_id" to businessId
            )
            val myBusinessDetails = hashMapOf(
                "business_name" to businessName,
                "business_logo" to businessLogo,
                "admin" to false,
                "is_accepted" to false,
                "user_position" to memberPosition,
                "approve_on" to FieldValue.serverTimestamp(),
                "business_id" to businessId
            )
            constants.db.collection("users")
                .document(constants.currentUserId.toString())
                .collection("my_work_place")
                .document(businessId.toString())
                .set(myBusinessDetails, SetOptions.merge())
                .addOnSuccessListener {
                    constants.db.collection("businesses")
                        .document(businessId.toString())
                        .collection("team")
                        .document(constants.currentUserId.toString())
                        .set(teamMemberDetails, SetOptions.merge())
                        .addOnSuccessListener {
                            progressDialog.hide()
                            finish()
                            Animatoo.animateFade(this)
                        }
                        .addOnFailureListener { e ->
                            Firebase.crashlytics.recordException(e)
                            progressDialog.hide()
                            Log.w(ContentValues.TAG, "Error writing document", e) }
                }
                .addOnFailureListener { e ->
                    Firebase.crashlytics.recordException(e)
                    progressDialog.hide()
                    Log.w(ContentValues.TAG, "Error writing document", e) }
        }
    }
}