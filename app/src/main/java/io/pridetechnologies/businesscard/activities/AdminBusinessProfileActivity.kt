package io.pridetechnologies.businesscard.activities

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.Department
import io.pridetechnologies.businesscard.EditBusinessAddressActivity
import io.pridetechnologies.businesscard.EditBusinessBasicsActivity
import io.pridetechnologies.businesscard.MainActivity
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.SocialMediaActivity
import io.pridetechnologies.businesscard.Team
import io.pridetechnologies.businesscard.databinding.ActivityAdminBusinesProfileBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import io.pridetechnologies.businesscard.databinding.DepartmentsCardBinding
import io.pridetechnologies.businesscard.databinding.TeamCardBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AdminBusinessProfileActivity : AppCompatActivity() {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityAdminBusinesProfileBinding
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
        binding = ActivityAdminBusinesProfileBinding.inflate(layoutInflater)
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

        binding.socialMediaButton.setOnClickListener {
            val intent = Intent(this, SocialMediaActivity::class.java)
            intent.putExtra("user_id", businessId.toString())
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.editAddressButton.setOnClickListener {
            val intent = Intent(this, EditBusinessAddressActivity::class.java)
            intent.putExtra("business_id", businessId.toString())
            startActivity(intent)
            Animatoo.animateFade(this)
        }

        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditBusinessBasicsActivity::class.java)
            intent.putExtra("business_id", businessId.toString())
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.deleteButton.setOnClickListener {
            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Delete Business Account"
            b.descTextView.text = "Are you sure you want to delete this business account?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                constants.db.collection("businesses").document(businessId.toString())
                    .delete()
                    .addOnCompleteListener {
                        dialog.dismiss()
                        finish()
                    }
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }

        getBusinessProfile()
        getDepartments()
        getTeam()
    }

    fun addDepartment(view: View) {
        val intent = Intent(this, AddDepartmentActivity::class.java)
        intent.putExtra("business_id",businessId)
        startActivity(intent)
        Animatoo.animateFade(this)
    }

    private fun getBusinessProfile() {
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { value, _ ->
                businessLogo = value?.get("business_logo").toString()
                businessName = value?.get("business_name").toString()
                val businessBio = value?.get("business_bio").toString()
                val businessCode = value?.get("business_qr_code").toString()
                val businessLink = value?.get("business_link").toString()
                val buildingNumber = value?.get("building_number").toString()
                val businessStreet = value?.get("street_name").toString()
                val businessArea = value?.get("area_located").toString()
                val businessDistrict = value?.get("district_name").toString()
                val businessCountry = value?.get("country").toString()

                if (!businessLogo.equals(null)){
                    Picasso.get().load(businessLogo).fit().centerCrop().placeholder(R.drawable.background_icon).into(binding.businessLogoView)
                }
                binding.businessNameView.text = businessName

                binding.textView52.text = buildingNumber
                binding.textView53.text = businessStreet
                binding.textView54.text = businessArea
                binding.textView55.text = businessDistrict
                binding.textView56.text = businessCountry

                binding.bioButton.setOnClickListener {
                    if (businessBio.isNotEmpty()){
                        val dialog = Dialog(this)
                        val dialogBinding: CustomBioDialogBinding = CustomBioDialogBinding.inflate(layoutInflater)
                        dialog.setContentView(dialogBinding.root)
                        dialogBinding.titleTextView.text = "Business Bio"
                        dialogBinding.bioTextView.text = businessBio
                        dialogBinding.closeButton.setOnClickListener { dialog.dismiss() }
                        dialog.setCancelable(true)
                        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }
                }
                binding.shareButton.setOnClickListener {
                    val dialog = Dialog(this)
                    val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                    dialog.setContentView(b.root)
                    b.textView29.text = "Business Card"
                    b.resetLayout.visibility = View.VISIBLE
                    Picasso.get().load(businessCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                    b.downloadButton.setOnClickListener {
                        myExecutor?.execute {
                            if (constants.hasInternetConnection(this)) {
                                myExecutor?.execute {
                                    myCodeBitmap = constants.downloadCode(this, businessCode)
                                    myHandler?.post {
                                        if(myCodeBitmap!=null){
                                            constants.saveMediaToStorage(this, myCodeBitmap, businessName.toString())
                                        }
                                    }
                                }
                            } else {
                                constants.showToast(this, "No Internet Connection")
                            }
                        }
                    }
                    b.copyLinkButton.setOnClickListener {
                        constants.copyText(this, businessLink)
                    }
                    b.resetButton.setOnClickListener {
                        progressDialog.show("Resetting code...")
                        val multiFormatWriter = MultiFormatWriter()
                        try {
                            val bitMatrix: BitMatrix = multiFormatWriter.encode(businessId.toString(), BarcodeFormat.QR_CODE, 300, 300)
                            val barcodeEncoder = BarcodeEncoder()
                            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
                            val bytes = ByteArrayOutputStream()
                            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                            val data = bytes.toByteArray()

                            val qrcodeRef = constants.storageRef.child("business_qr_code/${businessId}.jpg")
                            val uploadTask = qrcodeRef.putBytes(data)
                            uploadTask.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Get the download URL of the uploaded image
                                    qrcodeRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                                        val qrCodeDownloadUrl = uri?.toString()
                                        val myBusinessDetails = hashMapOf(
                                            "business_qr_code" to qrCodeDownloadUrl
                                        )
                                        constants.db.collection("businesses").document(businessId.toString())
                                            .set(myBusinessDetails, SetOptions.merge())
                                            .addOnSuccessListener {
                                                dialog.dismiss()
                                                progressDialog.hide()
                                                constants.showToast(this, "Code Reset Successful")
                                            }
                                            .addOnFailureListener { e ->
                                                dialog.dismiss()
                                                progressDialog.hide()
                                                constants.showToast(this, "Code Reset Unsuccessful")
                                            }

                                    }.addOnFailureListener {
                                        progressDialog.hide()
                                        // Handle any error that occurs while getting the download URL
                                        constants.showToast(this, "Failed to get the url")
                                    }
                                } else {
                                    progressDialog.hide()
                                    // Handle any error that occurs during upload
                                    constants.showToast(this, "Failed to upload qr code")
                                }
                            }
                        } catch (_: Exception) {
                            dialog.dismiss()
                            progressDialog.hide()
                            constants.showToast(this, "Failed to create qr code")
                        }
                    }
                    b.button10.setOnClickListener { dialog.dismiss() }
                    dialog.setCancelable(true)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }
            }
        constants.db.collection("social_media").document(businessId.toString())
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

                    if (facebookLink.equals("null")){
                        binding.facebookBtn.setColorFilter(R.color.darkPrimaryColor)
                    }else binding.facebookBtn.colorFilter = null
                    if (whatsAppLink.equals("null")){
                        binding.whatsAppBtn.setColorFilter(R.color.darkPrimaryColor)
                    }else binding.whatsAppBtn.colorFilter = null
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
                    binding.facebookBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.whatsAppBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.linkedInBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.twitterBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.youtubeBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.instagramBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.wechatBtn.setColorFilter(R.color.darkPrimaryColor)
                    binding.tiktokBtn.setColorFilter(R.color.darkPrimaryColor)
                    Log.d(ContentValues.TAG, "Current data: null")
                }

            }
    }

    private fun getTeam() {
        val query: Query = constants.db.collection("businesses").document(businessId.toString())
            .collection("team")

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
                    val intent = Intent(it.context, TeamMemberDetailsActivity::class.java)
                    intent.putExtra("member_id", model.member_id)
                    intent.putExtra("business_id", businessId)
                    intent.putExtra("business_name", businessName)
                    intent.putExtra("business_logo", businessLogo)
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
                    binding.noDepartmentsCardView.visibility = View.VISIBLE
                    binding.imageButton.visibility = View.GONE
                }else {
                    binding.noDepartmentsCardView.visibility = View.GONE
                    binding.imageButton.visibility = View.VISIBLE
                }
                return snapshots.size
            }
            override fun onBindViewHolder(holder: DepartmentsViewHolder, position: Int, model: Department) {
                holder.binding.departmentNameTextView.text = model.department_name
                holder.binding.departmentDescTextView.text = model.department_desc
                holder.binding.linearLayout3.visibility = View.VISIBLE
                holder.binding.editButton.setOnClickListener {
                    val intent = Intent(it.context, EditDepartmentActivity::class.java)
                    intent.putExtra("business_id", businessId)
                    intent.putExtra("department_id", model.department_id)
                    startActivity(intent)
                    Animatoo.animateFade(it.context)
                }
                holder.binding.deleteButton.setOnClickListener {
                    constants.db.collection("businesses").document(businessId.toString()).collection("departments").document(model.department_id.toString()).delete()
                }
                holder.binding.callButton.setOnClickListener {
                    if (!model.department_mobile.equals("null")) {
                        val number = String.format("tel: %s", model.department_mobile)
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.setData(Uri.parse(number))
                        Dexter.withContext(this@AdminBusinessProfileActivity)
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
                                Toast.makeText(this@AdminBusinessProfileActivity, "Error occurred! ", Toast.LENGTH_SHORT).show()
                            }
                            .onSameThread()
                            .check()
                    } else {
                        Toast.makeText(this@AdminBusinessProfileActivity, "No Mobile Number", Toast.LENGTH_LONG).show()
                    }
                }
                holder.binding.emailButton.setOnClickListener {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${model.department_email}"))
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                    startActivity(Intent.createChooser(emailIntent, "SEND_MAIL"))
                }
                holder.binding.whatsAppButton.setOnClickListener {
                    constants.openNumberInWhatsApp(this@AdminBusinessProfileActivity,"com.whatsapp",model.department_whatsapp.toString() )
                }
            }

        }
        departmentalContactsRecycler.adapter = adapter
        adapter.startListening()
    }

    class DepartmentsViewHolder(val binding: DepartmentsCardBinding) : RecyclerView.ViewHolder(binding.root)

    class TeamViewHolder(val binding: TeamCardBinding) : RecyclerView.ViewHolder(binding.root)

}