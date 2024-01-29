package io.pridetechnologies.businesscard

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.squareup.picasso.Picasso
import io.ktor.http.Url
import io.pridetechnologies.businesscard.databinding.ActivityUserRequestDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding
import nl.changer.audiowife.AudioWife
import java.io.IOException
import java.net.URL
import kotlin.math.ceil


class UserRequestDetailsActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityUserRequestDetailsBinding
    private val constants = Constants()
    private var photoUrl: String? = ""
    private var myPhotoUrl: String? = ""
    private var myName: String? = ""
    private var userName: String? = ""
    private var userId: String? = ""
    private var userFirstName: String? = ""
    private lateinit var noteUrl:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRequestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.declineRequestButton.setOnClickListener {
            declineRequest()
        }
        binding.acceptRequestButton.setOnClickListener {
            acceptRequest()
        }
        val verticalLayoutManager = LinearLayoutManager(this)
        binding.workPlaceRecycler.layoutManager = verticalLayoutManager

        userId = intent.getStringExtra("user_id")

        constants.db.collection("users").document(userId.toString())
            .get()
            .addOnSuccessListener { document ->
                userFirstName = document.getString("first_name")
                val userLastName = document.getString("surname")
                val profession = document.getString("profession")
                photoUrl  = document.getString("profile_image_url")

                userName = "$userFirstName $userLastName"

                Picasso.get().load(photoUrl).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView2)
                binding.textView6.text = userName
                binding.textView7.text = profession
            }
            .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e)
            }

        constants.db.collection("users").document(constants.currentUserId.toString())
            .collection("card_requests").document(userId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    noteUrl = snapshot.get("request_note").toString()
                    if (noteUrl != ""){
                        binding.noteLayout.visibility = View.VISIBLE
                        AudioWife.getInstance()
                            .init(this, Uri.parse(noteUrl))
                            .setPlayView(binding.play)
                            .setPauseView(binding.pause)
                            .setSeekBar(binding.mediaSeekbar)
                            .setRuntimeView(binding.runTime)
                            .setTotalTimeView(binding.totalTime)
                    }else {
                        binding.noteLayout.visibility = View.GONE
                    }
                }
            }

        constants.db.collection("social_media").document(userId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
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

                    if (facebookLink.equals("") || facebookLink.isEmpty()){
                        binding.facebookBtn.visibility =View.GONE
                    }else binding.facebookBtn.visibility =View.VISIBLE
                    if (whatsAppLink.equals("") || whatsAppLink.isEmpty()){
                        binding.whatsAppBtn.visibility =View.GONE
                    }else binding.whatsAppBtn.visibility =View.VISIBLE
                    if (linkedInLink.equals("") || linkedInLink.isEmpty()){
                        binding.linkedInBtn.visibility =View.GONE
                    }else binding.linkedInBtn.visibility =View.VISIBLE
                    if (twitterLink.equals("") || twitterLink.isEmpty()){
                        binding.twitterBtn.visibility =View.GONE
                    }else binding.twitterBtn.visibility =View.VISIBLE
                    if (youtubeLink.equals("") || youtubeLink.isEmpty()){
                        binding.youtubeBtn.visibility =View.GONE
                    }else binding.youtubeBtn.visibility =View.VISIBLE
                    if (instagramLink.equals("") || instagramLink.isEmpty()){
                        binding.instagramBtn.visibility =View.GONE
                    }else binding.instagramBtn.visibility =View.VISIBLE
                    if (weChatLink.equals("") || weChatLink.isEmpty()){
                        binding.wechatBtn.visibility =View.GONE
                    }else binding.wechatBtn.visibility =View.VISIBLE
                    if (tiktokLink.equals("") || tiktokLink.isEmpty()){
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
                    Log.d(TAG, "Current data: null")
                }

            }

        getMultipleWorkPlace()
    }

    private fun declineRequest() {
        val dialog = Dialog(this)
        val b = CustomDialogBoxBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        b.titleTextView.text = "Decline Request"
        b.descTextView.text = "Are you sure you don't want to share your contacts with $userFirstName?"
        b.positiveTextView.text = "Decline"
        b.positiveTextView.setOnClickListener {
            progressDialog.show("Declining Request...")
            constants.db.collection("users").document(constants.currentUserId.toString())
                .collection("card_requests").document(userId.toString()).delete()
                .addOnSuccessListener {
                    progressDialog.hide()
                    dialog.dismiss()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressDialog.hide()
                    dialog.dismiss()
                    Log.w(ContentValues.TAG, "Error writing document", e) }
        }
        b.negativeTextView.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun acceptRequest() {

        val dialog = Dialog(this)
        val b = CustomDialogBoxBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        b.titleTextView.text = "Approve Request"
        b.descTextView.text = "Are you sure you want to share your contacts with $userFirstName?"
        b.positiveTextView.text = "Approve"
        b.positiveTextView.setOnClickListener {
            progressDialog.show("Accepting Request...")
            val user = constants.auth.currentUser
            user?.let {
                myPhotoUrl = it.photoUrl.toString()
                myName = it.displayName
            }
            val userDetails = hashMapOf(
                "user_name" to userName,
                "user_image" to photoUrl,
                "user_id" to userId.toString()
            )
            val myDetails = hashMapOf(
                "user_name" to myName,
                "user_image" to myPhotoUrl,
                "user_id" to constants.currentUserId.toString()
            )

            constants.db.collection("users").document(constants.currentUserId.toString())
                .collection("individuals_cards").document(userId.toString()).set(userDetails, SetOptions.merge())

            constants.db.collection("users").document(userId.toString())
                .collection("individuals_cards").document(constants.currentUserId.toString())
                .set(myDetails, SetOptions.merge())
                .addOnSuccessListener {
                    constants.db.collection("users").document(constants.currentUserId.toString())
                        .collection("card_requests").document(userId.toString()).delete()
                    progressDialog.hide()
                    dialog.dismiss()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressDialog.hide()
                    dialog.dismiss()
                    Log.w(ContentValues.TAG, "Error writing document", e)
                }
        }
        b.negativeTextView.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun getMultipleWorkPlace() {
        val query: Query = FirebaseFirestore.getInstance()
            .collection("users").document(constants.currentUserId.toString())
            .collection("my_work_place")
            .whereEqualTo("is_accepted", true)

        val options: FirestoreRecyclerOptions<WorkCard> = FirestoreRecyclerOptions.Builder<WorkCard>()
            .setQuery(query, WorkCard::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<WorkCard, MyWorkPlaceViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyWorkPlaceViewHolder {
                val binding = WorkCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return MyWorkPlaceViewHolder(binding)
            }

            override fun onBindViewHolder(holder: MyWorkPlaceViewHolder, position: Int, model: WorkCard) {
                Picasso.get().load(model.business_logo).fit().centerCrop().into(holder.binding.logoImageView)
                holder.binding.positionTextView.text = model.user_position
                holder.binding.businessNameTextView.text = model.business_name
                constants.db.collection("businesses").document(model.business_id.toString())
                    .addSnapshotListener { value, _ ->
                        val businessLocation = value?.get("area_located").toString()
                        val businessDistrict = value?.get("district_name").toString()
                        val businessCountry = value?.get("country").toString()
                        holder.binding.businessLocationTextView.text = "$businessLocation, $businessDistrict, $businessCountry"
                    }
                holder.binding.button.setOnClickListener {
                    val intent = Intent(it.context, BusinessDetailsActivity::class.java)
                    intent.putExtra("business_id", model.business_id)
                    startActivity(intent)
                    Animatoo.animateFade(it.context)
                }
            }

        }
        val items = adapter.itemCount
        if (items == 0){
            binding.textView9.visibility = View.VISIBLE
        }else binding.textView9.visibility = View.VISIBLE
        binding.workPlaceRecycler.adapter = adapter
        adapter.startListening()
    }

    class MyWorkPlaceViewHolder(val binding: WorkCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onPause() {
        super.onPause()
        AudioWife.getInstance().release()
    }

}