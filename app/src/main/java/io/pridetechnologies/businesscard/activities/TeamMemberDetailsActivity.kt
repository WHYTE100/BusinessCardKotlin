package io.pridetechnologies.businesscard.activities

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.ActivityTeamMemberDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding

class TeamMemberDetailsActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityTeamMemberDetailsBinding
    private val constants = Constants()
    private var memberId: String? = ""
    private var businessId: String? = ""
    private var userFirstName: String? = ""
    private var otherName: String? = ""
    private var userLastName: String? = ""
    private var profession: String? = ""
    private var userImage: String? = ""
    private var businessLogo: String? = ""
    private var businessName: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamMemberDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        memberId = intent.getStringExtra("member_id")
        businessId = intent.getStringExtra("business_id")
        businessLogo = intent.getStringExtra("business_logo")
        businessName = intent.getStringExtra("business_name")

        constants.db.collection("users").document(memberId.toString())
            .get()
            .addOnSuccessListener { document ->
                userFirstName = document.getString("first_name")
                otherName = document.getString("other_names")
                userLastName = document.getString("surname")
                profession = document.getString("profession")
                userImage = document.getString("profile_image_url")

                Picasso.get().load(userImage).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView2)
                binding.textView6.text = "$userFirstName $userLastName"
                binding.textView7.text = profession
            }
            .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e)
            }
        constants.db.collection("users").document(memberId.toString())
            .collection("my_work_place").document(businessId.toString())
            .get()
            .addOnSuccessListener { document ->
                val isApproved = document.getBoolean("is_accepted")
                val isAdmin = document.getBoolean("admin")
                if (isApproved == true){
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.removeButton.visibility = View.VISIBLE
                }

            }
            .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e)
            }

        binding.approveButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Approve member"
            b.descTextView.text = "Are you sure you want to approve this member?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                removeMember()
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
        binding.declineButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Decline member"
            b.descTextView.text = "Are you sure you want to decline this member?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                declineMember()
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
        binding.removeButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Remove member"
            b.descTextView.text = "Are you sure you want to remove this member?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                removeMember()
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }

    private fun approveMember() {

        progressDialog.show("Adding Member...")
        val teamMemberDetails = hashMapOf(
            "user_name" to userFirstName,
            "user_image" to userImage,
            "admin" to false,
            "is_accepted" to true,
            "approve_on" to FieldValue.serverTimestamp(),
            "member_id" to memberId
        )
        val myBusinessDetails = hashMapOf(
            "business_name" to businessName,
            "business_logo" to businessLogo,
            "admin" to false,
            "is_accepted" to true,
            "approve_on" to FieldValue.serverTimestamp(),
            "business_id" to businessId
        )
        constants.db.collection("users")
            .document(memberId.toString())
            .collection("my_work_place")
            .document(businessId.toString())
            .set(myBusinessDetails, SetOptions.merge())
            .addOnSuccessListener {
                constants.db.collection("businesses")
                    .document(businessId.toString())
                    .collection("team")
                    .document(memberId.toString())
                    .set(teamMemberDetails, SetOptions.merge())
                    .addOnSuccessListener {
                        progressDialog.hide()
                        finish()
                        Animatoo.animateFade(this)
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Log.w(ContentValues.TAG, "Error writing document", e) }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }

    }

    private fun declineMember() {
        progressDialog.show("Declining Member...")
        constants.db.collection("users")
            .document(memberId.toString())
            .collection("my_work_place")
            .document(businessId.toString())
            .delete()
            .addOnSuccessListener {
                constants.db.collection("businesses")
                    .document(businessId.toString())
                    .collection("team")
                    .document(memberId.toString())
                    .delete()
                    .addOnSuccessListener {
                        progressDialog.hide()
                        finish()
                        Animatoo.animateFade(this)
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Log.w(ContentValues.TAG, "Error writing document", e) }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }
    }

    private fun removeMember() {

        progressDialog.show("Removing Member...")
        constants.db.collection("users")
            .document(memberId.toString())
            .collection("my_work_place")
            .document(businessId.toString())
            .delete()
            .addOnSuccessListener {
                constants.db.collection("businesses")
                    .document(businessId.toString())
                    .collection("team")
                    .document(memberId.toString())
                    .delete()
                    .addOnSuccessListener {
                        progressDialog.hide()
                        finish()
                        Animatoo.animateFade(this)
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Log.w(ContentValues.TAG, "Error writing document", e) }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }
    }
}