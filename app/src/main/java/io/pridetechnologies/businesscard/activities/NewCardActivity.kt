package io.pridetechnologies.businesscard.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.BusinessDetailsActivity
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.WorkCard
import io.pridetechnologies.businesscard.databinding.ActivityNewCardBinding
import io.pridetechnologies.businesscard.databinding.CustomRequestDialogBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding
import io.pridetechnologies.businesscard.notifications.NotificationData
import io.pridetechnologies.businesscard.notifications.PushNotification
import java.io.File
import java.io.IOException

class NewCardActivity : AppCompatActivity() {

    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityNewCardBinding
    private val constants = Constants()
    private var photoUrl: String? = ""
    private var userName: String? = ""
    private var userFirstName: String? = ""
    private var userProfession: String? = ""
    private var deepLink: String? = ""
    private var token: String? = ""

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaPlayer: MediaPlayer
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var isRecording = false

    private lateinit var countDownTimer: CountDownTimer
    private var counterSeconds = 0
    private lateinit var counterTextView : TextView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.RECORD_AUDIO
            )
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if(report.areAllPermissionsGranted()){

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

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.requestButton.setOnClickListener {

            val dialog = Dialog(this)
            val b = CustomRequestDialogBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            counterTextView = b.statusTextView
            b.noteTextView.text = "Before sending the request, you can let $userFirstName know why you want his/her contacts by recording a voice message. Click skip if that's not necessary. To record a note, press and hold the record button then release after you finish speaking."
            b.recordButton.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Handle the "down" motion event
                        b.recordButton.scaleX = 1.4f
                        b.recordButton.scaleY = 1.4f

                        startRecording()
                        b.playButton.visibility = View.GONE
                        b.stopButton.visibility = View.GONE

                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Handle the "up" motion event
                        b.recordButton.scaleX = 1.0f
                        b.recordButton.scaleY = 1.0f
                        stopRecording()
                        b.playButton.visibility = View.VISIBLE
                        b.stopButton.visibility = View.VISIBLE
                        b.sendButton.visibility = View.VISIBLE

                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Handle the "move" motion event
                        //val x = event.x
                        //val y = event.y
                        //println("Button moved to coordinates: ($x, $y)")
                        true
                    }
                    else -> false
                }
            }
            b.playButton.setOnClickListener {
                playAudio(b.statusTextView)
            }
            b.stopButton.setOnClickListener {
                stopAudio(b.statusTextView)
            }
            b.sendButton.setOnClickListener {
                dialog.dismiss()
                progressDialog.show("Sending Request...")
                val audioRef = constants.storageRef.child("request_note_audios/${constants.currentUserId}/$deepLink/${outputFile?.name}")
                val uploadTask = audioRef.putFile(outputUri!!)

                uploadTask.addOnSuccessListener {
                    // Audio upload success
                    // Get the download URL of the uploaded audio file
                    audioRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        val audioDownloadUrl = downloadUrl.toString()
                        sendCardRequest(audioDownloadUrl)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to retrieve download URL: ${exception.message}")
                    }
                }.addOnFailureListener { exception ->
                    // Audio upload failed
                    Log.e(TAG, "Audio upload failed: ${exception.message}")
                }
            }
            b.skipButton.setOnClickListener {
                dialog.dismiss()
                progressDialog.show("Sending Request...")
                sendCardRequest("") }
            dialog.setCancelable(true)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
        val verticalLayoutManager = LinearLayoutManager(this)
        binding.workPlaceRecycler.layoutManager = verticalLayoutManager

        deepLink = intent.getStringExtra("deepLink")
        constants.db.collection("users").document(deepLink.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    binding.userDetailsView.visibility = View.VISIBLE
                    userFirstName = snapshot.getString("first_name")
                    //val otherName = snapshot.getString("other_names")
                    token = snapshot.getString("token")
                    val userLastName = snapshot.getString("surname")
                    userProfession = snapshot.getString("profession")
                    val userImage = snapshot.getString("profile_image_url")

                    if (!userImage.equals("")){
                        Picasso.get().load(userImage).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView2)
                    }

                    binding.textView6.text = "$userFirstName $userLastName"
                    binding.textView7.text = userProfession
                }else{
                    binding.userDetailsView.visibility = View.GONE
                    checkIfBusinessCode()
                }
            }

        constants.db.collection("social_media").document(deepLink.toString())
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
                    Log.d(TAG, "Current data: null")
                }

            }
        getMultipleWorkPlace()
    }

    private fun startRecording() {
        try {
            outputFile = File.createTempFile("recording", ".mp3", cacheDir)
            outputUri = Uri.fromFile(outputFile)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startCounter()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        if (isRecording){
            mediaRecorder.apply {
                stop()
                release()
            }
        }
        isRecording = false
        stopCounter()
    }

    private fun playAudio(text: TextView) {

        mediaPlayer = MediaPlayer.create(this, outputUri)
        mediaPlayer.setOnPreparedListener { player ->
            player.start()
            text.text = "Playing Recording"
        }
        mediaPlayer.setOnCompletionListener { player ->
            player.release()
            text.text = "Play Again"
        }

    }

    private fun stopAudio(text: TextView) {
        mediaPlayer.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
            }
            text.text = "Play Stopped"
        }
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
                val binding = WorkCardBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return MyWorkPlaceViewHolder(binding)
            }

            override fun onBindViewHolder(holder: MyWorkPlaceViewHolder, position: Int, model: WorkCard) {
                if (!model.business_logo.equals(null)){
                    Picasso.get().load(model.business_logo).fit().centerCrop().placeholder(R.drawable.background_icon).into(holder.binding.logoImageView)
                }
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

    private fun sendCardRequest(noteUrl : String) {
        val user = constants.auth.currentUser
        user?.let {
            photoUrl = it.photoUrl.toString()
            userName = it.displayName
        }
        val requestDetails = hashMapOf(
            "sender_name" to userName,
            "sender_image" to photoUrl,
            "sender_profession" to userProfession,
            "sender_id" to user?.uid.toString(),
            "request_note" to noteUrl
        )

        constants.db.collection("users").document(deepLink.toString())
            .collection("card_requests").document(constants.currentUserId.toString())
            .set(requestDetails, SetOptions.merge())
            .addOnSuccessListener {
                PushNotification(NotificationData("Card Request", "Share your contacts with $userName.",constants.currentUserId.toString()), token.toString()).also { notification ->
                    constants.sendNotification(notification)
                }
                progressDialog.hide()
                binding.requestButton.isEnabled = false
                binding.textView99.visibility = View.VISIBLE
                binding.requestButton.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(TAG, "Error writing document", e) }

    }

    override fun onDestroy() {
        try {
            stopRecording()
            mediaPlayer.release()
            mediaRecorder.release()
        } catch (e: Exception){
            e.printStackTrace()
        }
        super.onDestroy()
        // Stop recording and release resources
    }

    override fun onStop() {
        try {
            stopRecording()
            mediaPlayer.release()
            mediaRecorder.release()
        } catch (e: Exception){
            e.printStackTrace()
        }

        super.onStop()

    }

    private fun startCounter() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                counterSeconds++
                updateCounterText()
            }

            override fun onFinish() {
                // Not needed for this example
            }
        }

        counterSeconds = 0
        countDownTimer.start()
    }

    private fun stopCounter() {
        countDownTimer.cancel()
    }

    private fun updateCounterText() {
        val minutes = counterSeconds / 60
        val seconds = counterSeconds % 60
        val counterText = String.format("%02d:%02d", minutes, seconds)
        counterTextView.text = counterText
        //Log.d(ContentValues.TAG, "Time : $counterText")
    }

    private fun checkIfBusinessCode() {
        constants.db.collection("businesses").document(deepLink.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val intent = Intent(this@NewCardActivity, NewBusinessCardActivity::class.java)
                    intent.putExtra("business_id", deepLink)
                    startActivity(intent)
                    finish()
                    Animatoo.animateFade(this@NewCardActivity)
                }else{
                    binding.invalidCodeView.visibility = View.VISIBLE
                    constants.showToast(this,"Invalid code")
                }
            }
    }
}
