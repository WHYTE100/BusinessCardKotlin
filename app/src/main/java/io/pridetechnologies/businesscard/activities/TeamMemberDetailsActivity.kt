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

        constants.db.collection("social_media").document(memberId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val whatsAppLink = snapshot.get("whatsapp_link").toString()
                    val facebookLink = snapshot.get("facebook_link").toString()
                    val linkedInLink = snapshot.get("linked_in_link").toString()
                    val twitterLink = snapshot.get("twitter_link").toString()
                    val youtubeLink = snapshot.get("youtube_link").toString()
                    val instagramLink = snapshot.get("instagram_link").toString()
                    val weChatLink = snapshot.get("wechat_link").toString()
                    val tiktokLink = snapshot.get("tiktok_link").toString()

                    if (facebookLink.equals("null") || facebookLink.isEmpty()){
                        binding.facebookBtn.visibility =View.GONE
                    }else binding.facebookBtn.visibility =View.VISIBLE
                    if (whatsAppLink.equals("null") || whatsAppLink.isEmpty()){
                        binding.whatsAppBtn.visibility =View.GONE
                    }else binding.whatsAppBtn.visibility =View.VISIBLE
                    if (linkedInLink.equals("null") || linkedInLink.isEmpty()){
                        binding.linkedInBtn.visibility =View.GONE
                    }else binding.linkedInBtn.visibility =View.VISIBLE
                    if (twitterLink.equals("null") || twitterLink.isEmpty()){
                        binding.twitterBtn.visibility =View.GONE
                    }else binding.twitterBtn.visibility =View.VISIBLE
                    if (youtubeLink.equals("null") || youtubeLink.isEmpty()){
                        binding.youtubeBtn.visibility =View.GONE
                    }else binding.youtubeBtn.visibility =View.VISIBLE
                    if (instagramLink.equals("null") || instagramLink.isEmpty()){
                        binding.instagramBtn.visibility =View.GONE
                    }else binding.instagramBtn.visibility =View.VISIBLE
                    if (weChatLink.equals("null") || weChatLink.isEmpty()){
                        binding.wechatBtn.visibility =View.GONE
                    }else binding.wechatBtn.visibility =View.VISIBLE
                    if (tiktokLink.equals("null") || tiktokLink.isEmpty()){
                        binding.tiktokBtn.visibility =View.GONE
                    }else binding.tiktokBtn.visibility =View.VISIBLE

                    binding.facebookBtn.setOnClickListener {
                        constants.openProfileInApp(this, "com.facebook.katana",facebookLink)
                    }
                    binding.whatsAppBtn.setOnClickListener{
                        constants.openNumberInWhatsApp(this,"com.whatsapp",whatsAppLink )
                    }
                    binding.linkedInBtn.setOnClickListener{
                        constants.openProfileInApp(this,"com.linkedin.android",linkedInLink )
                    }
                    binding.twitterBtn.setOnClickListener{
                        constants.openProfileInApp(this,"com.twitter.android",twitterLink )
                    }
                    binding.tiktokBtn.setOnClickListener{
                        constants.openProfileInApp(this,"com.zhiliaoapp.musically",tiktokLink )
                    }
                    binding.wechatBtn.setOnClickListener{
                        constants.openProfileInApp(this,"com.tencent.mm",weChatLink )
                    }
                    binding.instagramBtn.setOnClickListener{
                        constants.openProfileInApp(this,"com.whatsapp",instagramLink )
                    }
                    binding.youtubeBtn.setOnClickListener{
                        constants.openProfileInApp(this, "com.google.android.youtube",youtubeLink )
                    }
                } else {
                    Log.d(ContentValues.TAG, "Current data: null")
                }

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
                approveMember()
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

        checkIfAdmin()
    }

    private fun checkIfAdmin() {
        constants.db.collection("businesses")
            .document(businessId.toString())
            .collection("team")
            .document(memberId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val isAdmin = snapshot.getBoolean("admin")
                    val isSuperAdmin = snapshot.getBoolean("super_admin")

                    if (isAdmin == true){
                        if (isSuperAdmin == true){
                            binding.approveButton.visibility = View.GONE
                            binding.removeButton.visibility = View.GONE
                            binding.removeAdminButton.visibility = View.GONE
                            binding.makeAdminButton.visibility = View.GONE
                        }else{
                            binding.removeAdminButton.visibility = View.VISIBLE
                            binding.makeAdminButton.visibility = View.GONE
                        }
                    }else {
                        binding.makeAdminButton.visibility = View.VISIBLE
                        binding.removeAdminButton.visibility = View.GONE
                    }
                }
                binding.removeAdminButton.setOnClickListener {
                    updatedAdminStatus(false)
                }
                binding.makeAdminButton.setOnClickListener {
                    updatedAdminStatus(true)
                }
            }
    }

    private fun updatedAdminStatus(isAdmin: Boolean) {
        progressDialog.show("Adding Member as Admin")
        val teamMemberDetails = hashMapOf(
            "admin" to isAdmin
        )
        constants.db.collection("businesses").document(businessId.toString())
            .collection("team").document(constants.currentUserId.toString())
            .set(teamMemberDetails, SetOptions.merge())
            .addOnCompleteListener {task ->
                if (task.isSuccessful){
                    progressDialog.hide()
                    if (isAdmin == true){
                        binding.removeAdminButton.visibility = View.GONE
                        binding.makeAdminButton.visibility = View.VISIBLE
                    }else {
                        binding.makeAdminButton.visibility = View.GONE
                        binding.removeAdminButton.visibility = View.VISIBLE
                    }
                }else{
                    progressDialog.hide()
                    if (isAdmin == true){
                        constants.showToast(this,"Couldn't make member as admin.")
                    }else {
                        constants.showToast(this,"Couldn't remove member as admin.")
                    }
                }
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