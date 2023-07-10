package io.pridetechnologies.businesscard.activities

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.firebase.firestore.FieldValue
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
import io.pridetechnologies.businesscard.databinding.ActivityNewBusinessCardBinding
import io.pridetechnologies.businesscard.databinding.ActivityUserProfileBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NewBusinessCardActivity : AppCompatActivity() {

    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityNewBusinessCardBinding
    private val constants = Constants()
    private var businessId: String? = ""
    private var businessName: String? = ""
    private var businessLogo: String? = ""
    private var myExecutor: ExecutorService? = null
    private var myHandler: Handler? = null
    private var myCodeBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewBusinessCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myExecutor = Executors.newSingleThreadExecutor()
        myHandler = Handler(Looper.getMainLooper())

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        businessId = intent.getStringExtra("business_id")

        binding.businessBioView
            .setAnimationDuration(500)
            .setReadMoreText("More")
            .setReadLessText("Less")
            .setCollapsedLines(3)
            .setIsExpanded(false)
            .setIsUnderlined(true)
            .setEllipsizedTextColor(ContextCompat.getColor(this, R.color.darkSecondaryDarkColor))

        binding.businessBioView.setOnClickListener {
                binding.businessBioView.toggle()
        }

        binding.seeMoreButton.setOnClickListener {
            val intent = Intent(this, BusinessDetailsActivity::class.java)
            intent.putExtra("business_id", businessId)
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.addButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Add Business"
            b.descTextView.text = "Do you want to add this business contacts to your account?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                addToMyBusinessCards()
                dialog.dismiss()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
        getBusinessProfile()
    }

    private fun addToMyBusinessCards() {
        progressDialog.show("Adding Business...")

        val businessDetails = hashMapOf(
            "business_name" to businessName,
            "business_logo" to businessLogo,
            "added_on" to FieldValue.serverTimestamp(),
            "business_id" to businessId
        )

        constants.db.collection("users").document(constants.currentUserId.toString())
            .collection("businesses_cards").document(businessId.toString())
            .set(businessDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                binding.addButton.isEnabled = false
                binding.addButton.text = "Business Added"
                //Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }
    }

    private fun getBusinessProfile() {
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    binding.businessDetailsView.visibility = View.VISIBLE
                    businessLogo = snapshot.get("business_logo").toString()
                    businessName = snapshot.get("business_name").toString()
                    val businessBio = snapshot.get("business_bio").toString()
                    val businessMobile = snapshot.get("business_mobile").toString()
                    val businessWebsite = snapshot.get("business_website").toString()
                    val businessEmail = snapshot.get("business_email").toString()
                    val businessCode = snapshot.get("business_qr_code").toString()
                    val businessLink = snapshot.get("business_link").toString()
                    val areaLocated = snapshot.get("area_located").toString()
                    val districtName = snapshot.get("district_name").toString()
                    val country = snapshot.get("country").toString()

                    if (!businessLogo.equals(null)){
                        Picasso.get().load(businessLogo).fit().centerCrop().placeholder(R.drawable.background_icon).into(binding.businessLogoView)
                    }
                    binding.businessNameView.text = businessName
                    binding.businessBioView.text = businessBio
                    binding.businessAddressView.text = "${areaLocated}, ${districtName}, ${country}"

                    if (businessMobile.equals("null") || businessMobile.isEmpty()){
                        binding.callBtn.visibility = View.GONE
                    }
                    if (businessWebsite.equals("null") || businessWebsite.isEmpty()){
                        binding.bioBtn.visibility = View.GONE
                    }
                    if (businessEmail.equals("null") || businessEmail.isEmpty()){
                        binding.emailBtn.visibility = View.GONE
                    }
                    if (businessBio.equals("null") || businessBio.isEmpty()){
                        binding.businessBioView.visibility = View.GONE
                    }

                    binding.callButton.setOnClickListener {
                        if (!businessMobile.equals("null")) {
                            val number = String.format("tel: %s", businessMobile)
                            val callIntent = Intent(Intent.ACTION_CALL)
                            callIntent.setData(Uri.parse(number))
                            Dexter.withContext(this)
                                .withPermissions(android.Manifest.permission.CALL_PHONE)
                                .withListener(object : MultiplePermissionsListener {
                                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                        // check if all permissions are granted
                                        if (report.areAllPermissionsGranted()) {
                                            startActivity(callIntent)
                                        }
                                    }

                                    override fun onPermissionRationaleShouldBeShown(
                                        permissions: List<PermissionRequest?>?,
                                        token: PermissionToken
                                    ) {
                                        token.continuePermissionRequest()
                                    }
                                })
                                .withErrorListener {
                                    Toast.makeText(this, "Error occurred! ", Toast.LENGTH_SHORT).show()
                                }
                                .onSameThread()
                                .check()
                        } else {
                            Toast.makeText(this, "No Mobile Number", Toast.LENGTH_LONG).show()
                        }
                    }
                    binding.whatsAppButton.setOnClickListener {
                        if (!businessMobile.equals("null")) {
                            constants.openNumberInWhatsApp(this, "com.whatsapp", businessMobile)
                        }
                    }
                    binding.emailButton.setOnClickListener {
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$businessEmail"))
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                        startActivity(Intent.createChooser(emailIntent, "SEND_MAIL"))
                    }
                    binding.websiteButton.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(businessWebsite))
                        startActivity(intent)
                    }
                    binding.shareButton.setOnClickListener {

                        val dialog = Dialog(this)
                        val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                        dialog.setContentView(b.root)
                        b.textView29.text = "Business Card"
                        Picasso.get().load(businessCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                        b.downloadButton.setOnClickListener {
                            myExecutor?.execute {
                                myCodeBitmap = constants.downloadCode(this, businessCode)
                                myHandler?.post {
                                    if(myCodeBitmap!=null){
                                        constants.saveMediaToStorage(this, myCodeBitmap, businessName.toString())
                                    }
                                }
                            }
                        }
                        b.copyLinkButton.setOnClickListener {
                            constants.copyText(this, businessLink)
                        }
                        b.button10.setOnClickListener { dialog.dismiss() }
                        dialog.setCancelable(true)
                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }
                }else {
                    binding.businessDetailsView.visibility = View.GONE
                    checkIfIndividualCode()
                }
            }
        constants.db.collection("social_media").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                val whatsAppLink = snapshot?.get("whatsapp_link").toString()
                val facebookLink = snapshot?.get("facebook_link").toString()
                val linkedInLink = snapshot?.get("linked_in_link").toString()
                val twitterLink = snapshot?.get("twitter_link").toString()
                val youtubeLink = snapshot?.get("youtube_link").toString()
                val instagramLink = snapshot?.get("instagram_link").toString()
                val weChatLink = snapshot?.get("wechat_link").toString()
                val tiktokLink = snapshot?.get("tiktok_link").toString()

                if (facebookLink.equals("null") || facebookLink.isEmpty()){
                    binding.facebookBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.facebookBtn.colorFilter = null
                if (whatsAppLink.equals("null") || whatsAppLink.isEmpty()){
                    binding.whatsAppBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.whatsAppBtn.colorFilter = null
                if (linkedInLink.equals("null") || linkedInLink.isEmpty()){
                    binding.linkedInBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.linkedInBtn.colorFilter = null
                if (twitterLink.equals("null") || twitterLink.isEmpty()){
                    binding.twitterBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.twitterBtn.colorFilter = null
                if (youtubeLink.equals("null") || youtubeLink.isEmpty()){
                    binding.youtubeBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.youtubeBtn.colorFilter = null
                if (instagramLink.equals("null") || instagramLink.isEmpty()){
                    binding.instagramBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.instagramBtn.colorFilter = null
                if (weChatLink.equals("null") || weChatLink.isEmpty()){
                    binding.wechatBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.wechatBtn.colorFilter = null
                if (tiktokLink.equals("null") || tiktokLink.isEmpty()){
                    binding.tiktokBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.tiktokBtn.colorFilter = null

                binding.facebookBtn.setOnClickListener {
                    constants.openProfileInApp(this, "com.facebook.katana", facebookLink)
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

            }
    }

    private fun checkIfIndividualCode() {
        constants.db.collection("users").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val intent = Intent(this@NewBusinessCardActivity, NewCardActivity::class.java)
                    intent.putExtra("deepLink", businessId)
                    startActivity(intent)
                    finish()
                    Animatoo.animateFade(this@NewBusinessCardActivity)
                }else{
                    binding.invalidCodeView.visibility = View.VISIBLE
                    constants.showToast(this,"Invalid code")
                }
            }
    }
}