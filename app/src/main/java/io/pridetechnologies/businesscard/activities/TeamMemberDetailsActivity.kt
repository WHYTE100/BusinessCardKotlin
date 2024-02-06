package io.pridetechnologies.businesscard.activities

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.databinding.ActivityTeamMemberDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.notifications.NotificationData
import io.pridetechnologies.businesscard.notifications.PushNotification
import kotlinx.coroutines.launch

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
    private var token: String? = ""

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
                token = document.getString("token")

                Picasso.get().load(userImage).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView2)
                binding.textView6.text = "$userFirstName $userLastName"
                binding.textView7.text = profession
            }
            .addOnFailureListener { e -> Firebase.crashlytics.recordException(e)
            }

        constants.db.collection("social_media").document(memberId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Firebase.crashlytics.recordException(e)
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
                    binding.makeAdminButton.visibility = View.VISIBLE
                }
                if (isAdmin == true){
                    binding.removeAdminButton.visibility = View.VISIBLE
                    binding.removeButton.visibility = View.VISIBLE
                    binding.makeAdminButton.visibility = View.GONE
                }

            }
            .addOnFailureListener { e -> Firebase.crashlytics.recordException(e)
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
        binding.removeAdminButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Remove as Admin"
            b.descTextView.text = "Are you sure you want to remove this member as an admin?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                updatedAdminStatus(false)
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }

        binding.makeAdminButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Make Admin"
            b.descTextView.text = "Are you sure you want to make this member as an admin?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                updatedAdminStatus(true)
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
                    Firebase.crashlytics.recordException(e)
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
                            binding.removeButton.visibility = View.VISIBLE
                            binding.makeAdminButton.visibility = View.GONE
                        }
                    }else {
                        binding.makeAdminButton.visibility = View.VISIBLE
                        binding.removeAdminButton.visibility = View.GONE
                        binding.removeButton.visibility = View.VISIBLE
                    }
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
                        binding.removeAdminButton.visibility = View.VISIBLE
                        binding.removeButton.visibility = View.VISIBLE
                        binding.makeAdminButton.visibility = View.GONE
                    }else {
                        binding.makeAdminButton.visibility = View.VISIBLE
                        binding.removeAdminButton.visibility = View.GONE
                        binding.removeButton.visibility = View.VISIBLE
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
                        lifecycleScope.launch {
                            PushNotification(NotificationData("Join Team Request", "Your profile has been linked to $businessName business profile.", "accepted_team_request",businessId.toString()), token.toString()).also { notification ->
                                val result = constants.sendNotification(notification)
                                result.onSuccess {
                                    progressDialog.hide()
                                    finish()
                                    Animatoo.animateFade(this@TeamMemberDetailsActivity)
                                }.onFailure {
                                    Firebase.crashlytics.recordException(it)
                                    progressDialog.hide()
                                    finish()
                                    Animatoo.animateFade(this@TeamMemberDetailsActivity)
                                }
                            }

                        }
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Firebase.crashlytics.recordException(e) }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Firebase.crashlytics.recordException(e)
            }

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
                        lifecycleScope.launch {
                            PushNotification(NotificationData("Join Team Request", "Your profile hasn't been linked to $businessName business profile. The admin has declined your request. Contact the admin to solve this issue.", "declined_team_request",businessId.toString()), token.toString()).also { notification ->
                                val result = constants.sendNotification(notification)
                                result.onSuccess { response ->
                                    progressDialog.hide()
                                    finish()
                                    Animatoo.animateFade(this@TeamMemberDetailsActivity)
                                }.onFailure { e ->
                                    Firebase.crashlytics.recordException(e)
                                    progressDialog.hide()
                                    finish()
                                    Animatoo.animateFade(this@TeamMemberDetailsActivity)
                                }
                            }

                        }
                    }
                    .addOnFailureListener { e ->
                        progressDialog.hide()
                        Firebase.crashlytics.recordException(e) }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Firebase.crashlytics.recordException(e) }
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
                        Firebase.crashlytics.recordException(e)
                        progressDialog.hide()
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Firebase.crashlytics.recordException(e)
            }
    }
}