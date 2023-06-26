package io.pridetechnologies.businesscard

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.activities.EditDepartmentActivity
import io.pridetechnologies.businesscard.activities.NewCardActivity
import io.pridetechnologies.businesscard.activities.TeamMemberDetailsActivity
import io.pridetechnologies.businesscard.databinding.ActivityAdminBusinesProfileBinding
import io.pridetechnologies.businesscard.databinding.ActivityBusinessDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import io.pridetechnologies.businesscard.databinding.DepartmentsCardBinding
import io.pridetechnologies.businesscard.databinding.TeamCardBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BusinessDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusinessDetailsBinding
    private val constants = Constants()
    private lateinit var departmentalContactsRecycler: RecyclerView
    private lateinit var teamRecycler: RecyclerView
    private var businessId: String? = ""
    private var businessName: String? = ""
    private var businessLogo: String? = ""
    private var myExecutor: ExecutorService? = null
    private var myHandler: Handler? = null
    private var myCodeBitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myExecutor = Executors.newSingleThreadExecutor()
        myHandler = Handler(Looper.getMainLooper())

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        businessId = intent.getStringExtra("business_id")

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        teamRecycler = binding.teamRecycler
        teamRecycler.layoutManager = layoutManager

        val verticalLayoutManager = LinearLayoutManager(this)
        departmentalContactsRecycler = binding.departmentalContactsRecyclerView
        departmentalContactsRecycler.layoutManager = verticalLayoutManager

        getBusinessProfile()
        getDepartments()
        getTeam()
    }
    private fun getBusinessProfile() {
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                businessLogo = snapshot?.get("business_logo").toString()
                businessName = snapshot?.get("business_name").toString()
                val businessBio = snapshot?.get("business_bio").toString()
                val businessMobile = snapshot?.get("business_mobile").toString()
                val businessWebsite = snapshot?.get("business_website").toString()
                val businessEmail = snapshot?.get("business_email").toString()
                val businessCode = snapshot?.get("business_qr_code").toString()
                val businessLink = snapshot?.get("business_link").toString()

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

                Picasso.get().load(businessLogo).fit().centerCrop().placeholder(R.drawable.background_icon).into(binding.businessLogoView)
                binding.businessNameView.text = businessName
                binding.businessBioView.text = businessBio

                binding.callButton.setOnClickListener {
                    if (!businessMobile.equals("null") || businessMobile.isNotEmpty()) {
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
                    if (!businessMobile.equals("null") || businessMobile.isNotEmpty()) {
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

                    if (!businessBio.equals("null") || businessWebsite.isNotEmpty()){
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(businessWebsite))
                        startActivity(intent)
                    }else Toast.makeText(this, "No Website", Toast.LENGTH_SHORT).show()
                }
                binding.shareButton.setOnClickListener {

                    val dialog = Dialog(this)
                    val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                    dialog.setContentView(b.root)
                    b.textView29.text = "Business Card"
                    Picasso.get().load(businessCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                    b.downloadButton.setOnClickListener {
                        //val destinationPath = File(this.filesDir, "${businessName}.jpg")
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

                if (snapshot != null && snapshot.exists()) {
                    val buildingNumber = snapshot.get("building_number").toString()
                    val streetName = snapshot.get("street_name").toString()
                    val areaLocated = snapshot.get("area_located").toString()
                    val districtName = snapshot.get("district_name").toString()
                    val country = snapshot.get("country").toString()
                    val businessLatitude = snapshot.getDouble("business_latitude") ?: 0.0
                    val businessLongitude = snapshot.getDouble("business_longitude") ?: 0.0

                    binding.businessAddressView.text = "${areaLocated}, ${districtName}, ${country} "
                    binding.textView52.text = buildingNumber
                    binding.textView53.text = streetName
                    binding.textView54.text = areaLocated
                    binding.textView55.text = districtName
                    binding.textView56.text = country

                    binding.directionsButton.setOnClickListener {
                        if (businessLatitude != 0.0 && businessLongitude != 0.0){
                            constants.openGoogleMapsAndDirections(this, businessLatitude, businessLongitude)
                        }else {binding.directionsButton.visibility = View.GONE}
                    }
                }

            }
        constants.db.collection("social_media").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                //val whatsAppLink = snapshot?.get("whatsapp_link").toString()
                val facebookLink = snapshot?.get("facebook_link").toString()
                val linkedInLink = snapshot?.get("linked_in_link").toString()
                val twitterLink = snapshot?.get("twitter_link").toString()
                val youtubeLink = snapshot?.get("youtube_link").toString()
                val instagramLink = snapshot?.get("instagram_link").toString()
                val weChatLink = snapshot?.get("wechat_link").toString()
                val tiktokLink = snapshot?.get("tiktok_link").toString()

                if (facebookLink.equals("null")){
                    binding.facebookBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.facebookBtn.colorFilter = null
//                    if (whatsAppLink.equals("null")){
//                        binding.whatsAppBtn.setColorFilter(R.color.darkPrimaryColor)
//                    }else binding.whatsAppBtn.colorFilter = null
                if (linkedInLink.equals("null")){
                    binding.linkedInBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.linkedInBtn.colorFilter = null
                if (twitterLink.equals("null")){
                    binding.twitterBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.twitterBtn.colorFilter = null
                if (youtubeLink.equals("null")){
                    binding.youtubeBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.youtubeBtn.colorFilter = null
                if (instagramLink.equals("null")){
                    binding.instagramBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.instagramBtn.colorFilter = null
                if (weChatLink.equals("null")){
                    binding.wechatBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.wechatBtn.colorFilter = null
                if (tiktokLink.equals("null")){
                    binding.tiktokBtn.setColorFilter(R.color.darkPrimaryColor)
                }else binding.tiktokBtn.colorFilter = null

                binding.facebookBtn.setOnClickListener {
                    constants.openProfileInApp(this, "com.facebook.katana", facebookLink)
                }
//                    binding.whatsAppBtn.setOnClickListener{
//                        constants.openNumberInWhatsApp(this,"com.whatsapp",whatsAppLink )
//                    }
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

    private fun getTeam() {
        val query: Query = constants.db.collection("businesses").document(businessId.toString())
            .collection("team")
            .whereEqualTo("is_accepted", true)

        val options: FirestoreRecyclerOptions<Team> = FirestoreRecyclerOptions.Builder<Team>()
            .setQuery(query, Team::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Team, TeamViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
                val binding = TeamCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return TeamViewHolder(binding)
            }

            override fun onBindViewHolder(holder: TeamViewHolder, position: Int, model: Team) {
                Picasso.get().load(model.user_image).fit().centerCrop().into(holder.binding.userImageView)
                holder.binding.userNameTextView.text = model.user_name
                holder.binding.userPositionTextView.text = model.user_position
                holder.binding.root.setOnClickListener {
                    val intent = Intent(this@BusinessDetailsActivity, NewCardActivity::class.java)
                    intent.putExtra("deepLink", model.member_id)
                    startActivity(intent)
                    Animatoo.animateFade(it.context)
                }
            }

        }
        teamRecycler.adapter = adapter
        adapter.startListening()
    }

    private fun getDepartments() {
        val query: Query = constants.db.collection("businesses").document(businessId.toString())
            .collection("departments")

        val options: FirestoreRecyclerOptions<Department> = FirestoreRecyclerOptions.Builder<Department>()
            .setQuery(query, Department::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Department, DepartmentsViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentsViewHolder {
                val binding = DepartmentsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return DepartmentsViewHolder(binding)
            }
            override fun getItemCount(): Int {
                if(snapshots.size == 0){
                    binding.textView14.visibility = View.GONE
                }else {
                    binding.textView14.visibility = View.VISIBLE
                }
                return snapshots.size
            }
            override fun onBindViewHolder(holder: DepartmentsViewHolder, position: Int, model: Department) {
                holder.binding.departmentNameTextView.text = model.department_name
                holder.binding.departmentDescTextView.text = model.department_desc
                holder.binding.callButton.setOnClickListener {
                    if (!model.department_mobile.equals("null")) {
                        val number = String.format("tel: %s", model.department_mobile)
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.setData(Uri.parse(number))
                        Dexter.withContext(this@BusinessDetailsActivity)
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
                                Toast.makeText(this@BusinessDetailsActivity, "Error occurred! ", Toast.LENGTH_SHORT).show()
                            }
                            .onSameThread()
                            .check()
                    } else {
                        Toast.makeText(this@BusinessDetailsActivity, "No Mobile Number", Toast.LENGTH_LONG).show()
                    }
                }
                holder.binding.emailButton.setOnClickListener {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${model.department_email}"))
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                    startActivity(Intent.createChooser(emailIntent, "SEND_MAIL"))
                }
                holder.binding.whatsAppButton.setOnClickListener {
                    constants.openNumberInWhatsApp(this@BusinessDetailsActivity,"com.whatsapp",model.department_whatsapp.toString() )
                }
            }

        }
        departmentalContactsRecycler.adapter = adapter
        adapter.startListening()
    }

    class DepartmentsViewHolder(val binding: DepartmentsCardBinding) : RecyclerView.ViewHolder(binding.root)

    class TeamViewHolder(val binding: TeamCardBinding) : RecyclerView.ViewHolder(binding.root)
}