package io.pridetechnologies.businesscard

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.activities.AddAnotherBusinessActivity
import io.pridetechnologies.businesscard.activities.AdminBusinessProfileActivity
import io.pridetechnologies.businesscard.activities.NewBusinessActivity
import io.pridetechnologies.businesscard.databinding.ActivityUserProfileBinding
import io.pridetechnologies.businesscard.databinding.CustomAddCardBinding
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.CustomLinkBusinessCardBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import io.pridetechnologies.businesscard.databinding.RequestCardBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class UserProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserProfileBinding
    private val constants = Constants()
    private lateinit var requestRecycler: RecyclerView
    private lateinit var workPlaceRecycler: RecyclerView

    private var businessId: String? = null
    private var qrScan: IntentIntegrator? = null
    private var result: IntentResult? = null
    private var myExecutor:ExecutorService? = null
    private var myHandler: Handler? = null
    private var myCodeBitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myExecutor = Executors.newSingleThreadExecutor()
        myHandler = Handler(Looper.getMainLooper())

        binding.backButton.setOnClickListener {
            finish()
        }
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        requestRecycler = binding.requestRecyclerView
        requestRecycler.layoutManager = layoutManager

        val verticalLayoutManager = LinearLayoutManager(this)
        workPlaceRecycler = binding.workPlaceRecycler
        workPlaceRecycler.layoutManager = verticalLayoutManager

        val user = constants.auth.currentUser
        user?.let {
            val photoUrl = it.photoUrl
            val userName = it.displayName
            Picasso.get().load(photoUrl).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.userImageView)
            binding.userNameView.text = userName
        }

        binding.shareButton.setOnClickListener {
            //val intent = Intent(this, SocialMediaActivity::class.java)
            //startActivity(intent)
            //Animatoo.animateFade(this)
        }
        binding.editButton.setOnClickListener {
            val intent = Intent(this, EditUserDetailsActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(this)
        }

        binding.addNewButton.setOnClickListener {
            val intent = Intent(this, NewBusinessActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.linkButton.setOnClickListener {
            linkBusiness()
        }
        binding.imageButton.setOnClickListener {
            val intent = Intent(this, AddAnotherBusinessActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(this)
        }
        binding.socialMediaButton.setOnClickListener {
            val intent = Intent(this, SocialMediaActivity::class.java)
            intent.putExtra("user_id", constants.currentUserId.toString())
            startActivity(intent)
            Animatoo.animateFade(this)
        }

        binding.signOutButton.setOnClickListener {

            val dialog = Dialog(this)
            val b = CustomDialogBoxBinding.inflate(layoutInflater)
            dialog.setContentView(b.root)
            b.titleTextView.text = "Sign Out"
            b.descTextView.text = "You want to sign out?"
            b.positiveTextView.text = "Yes"
            b.positiveTextView.setOnClickListener {
                constants.auth.signOut()
                constants.db.terminate()
                dialog.dismiss()
                constants.writeToSharedPreferences(this,"user_qr_code", "")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                Animatoo.animateFade(this)
                finishAffinity()
            }
            b.negativeTextView.setOnClickListener { dialog.dismiss() }
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }

        getUserProfile()
        getCardRequest()
        getMyWorkPlace()

    }

    private fun linkBusiness() {
        val dialog = Dialog(this)
        val dialogBinding: CustomLinkBusinessCardBinding = CustomLinkBusinessCardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.scanBusinessButton.setOnClickListener {
            qrScan = IntentIntegrator(this)
            qrScan!!.setOrientationLocked(true)
            qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            qrScan!!.setPrompt("Scan QR code")
            qrScan!!.setCameraId(0) // Use a specific camera of the device
            qrScan!!.setBeepEnabled(true)
            qrScan!!.setBarcodeImageEnabled(true)
            qrScan!!.initiateScan()
        }
        dialogBinding.galleyBusinessButton.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            startActivityForResult(pickIntent, HomeActivity.BUSINESS_REQUEST_GALLERY_PHOTO)
        }
        dialogBinding.button10.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(true)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun getUserProfile() {
        constants.db.collection("users").document(constants.currentUserId.toString())
            .addSnapshotListener { value, _ ->
                val userProfession = value?.get("profession").toString()
                val userLink = value?.get("user_link").toString()

                val code = constants.readFromSharedPreferences(this,"user_qr_code", "")
                if (code.equals("null") || code.isEmpty()){
                    val encodedCode = value?.get("encode_qr_code").toString()
                    constants.writeToSharedPreferences(this,"user_qr_code", encodedCode)
                }
                binding.userProfessionView.text = userProfession


                val userBio = value?.get("user_bio").toString()
                binding.bioButton.setOnClickListener {
                    if (userBio.isNotEmpty()){
                        val dialog = Dialog(this)
                        val dialogBinding: CustomBioDialogBinding = CustomBioDialogBinding.inflate(layoutInflater)
                        dialog.setContentView(dialogBinding.root)
                        dialogBinding.bioTextView.text = userBio
                        dialogBinding.closeButton.setOnClickListener { dialog.dismiss() }
                        dialog.setCancelable(true)
                        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.show()
                    }
                }
                val userCode = value?.get("user_qr_code").toString()
                binding.shareButton.setOnClickListener {
                    val dialog = Dialog(this)
                    val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                    dialog.setContentView(b.root)
                    b.textView29.text = getString(R.string.my_card)
                    b.resetLayout.visibility = View.VISIBLE
                    Picasso.get().load(userCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                    b.downloadButton.setOnClickListener {
                        myExecutor?.execute {
                            myCodeBitmap = constants.downloadCode(this, userCode)
                            myHandler?.post {
                                if(myCodeBitmap!=null){
                                    constants.saveMediaToStorage(this, myCodeBitmap, "MyCode")
                                }
                            }
                        }
                    }
                    b.copyLinkButton.setOnClickListener {
                        constants.copyText(this, userLink)
                    }
                    b.resetButton.setOnClickListener {
                        val multiFormatWriter = MultiFormatWriter()
                        try {
                            val bitMatrix: BitMatrix = multiFormatWriter.encode(constants.currentUserId.toString(), BarcodeFormat.QR_CODE, 300, 300)
                            val barcodeEncoder = BarcodeEncoder()
                            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
                            val bytes = ByteArrayOutputStream()
                            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                            val data = bytes.toByteArray()
                            val encodedCode = Base64.encodeToString(data, Base64.DEFAULT)
                            constants.writeToSharedPreferences(this,"user_qr_code", encodedCode)

                            val qrcodeRef = constants.storageRef.child("user_qr_code/${constants.currentUserId}.jpg")
                            val uploadTask = qrcodeRef.putBytes(data)
                            uploadTask.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Get the download URL of the uploaded image
                                    qrcodeRef.downloadUrl.addOnSuccessListener { uri: Uri? ->
                                        val qrCodeDownloadUrl = uri?.toString()
                                        val myDetails = hashMapOf(
                                            "encode_qr_code" to encodedCode,
                                            "user_qr_code" to qrCodeDownloadUrl
                                        )
                                        constants.db.collection("users").document(constants.currentUserId.toString())
                                            .set(myDetails, SetOptions.merge())
                                            .addOnSuccessListener {
                                                dialog.dismiss()
                                                Toast.makeText(this, "Code Reset Successful", Toast.LENGTH_SHORT).show()
                                                //constants.setSnackBar(this, "Reset Successful")
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error: $e", Toast.LENGTH_SHORT).show()
                                                dialog.dismiss()
                                                //constants.setSnackBar(this, "Reset Unsuccessful")
                                            }

                                    }.addOnFailureListener {
                                        Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
                                        // Handle any error that occurs while getting the download URL
                                        //constants.setSnackBar(this, "Failed to get the url")
                                    }
                                } else {
                                    // Handle any error that occurs during upload
                                    //constants.setSnackBar(this, "Failed to upload qr code")
                                    Toast.makeText(this, "Failed to upload qr code", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (_: Exception) {
                            dialog.dismiss()
                            //constants.setSnackBar(this, "Failed to create qr code")
                        }
                    }
                    b.button10.setOnClickListener { dialog.dismiss() }
                    dialog.setCancelable(true)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }
            }
        constants.db.collection("social_media").document(constants.currentUserId.toString())
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
                    Log.d(ContentValues.TAG, "Current data: null")
                }

            }
    }

    private fun getMyWorkPlace() {
        getMultipleWorkPlace()
    }

    private fun getMultipleWorkPlace() {
        val workPlaceQuery: Query = FirebaseFirestore.getInstance()
            .collection("users").document(constants.currentUserId.toString())
            .collection("my_work_place")
            .whereEqualTo("is_accepted", true)

        val workPlaceOptions: FirestoreRecyclerOptions<WorkCard> = FirestoreRecyclerOptions.Builder<WorkCard>()
            .setQuery(workPlaceQuery, WorkCard::class.java)
            .build()

        val workPlaceAdapter = object : FirestoreRecyclerAdapter<WorkCard, MyWorkPlaceViewHolder>(workPlaceOptions){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyWorkPlaceViewHolder {
                val binding = WorkCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return MyWorkPlaceViewHolder(binding)
            }

            override fun getItemCount(): Int {
                if(snapshots.size == 0){
                    binding.noWorkProfileCardView.visibility = View.VISIBLE
                    binding.imageButton.visibility = View.GONE
                }else {
                    binding.noWorkProfileCardView.visibility = View.GONE
                    binding.imageButton.visibility = View.VISIBLE
                }
                return snapshots.size
            }

            override fun onBindViewHolder(holder: MyWorkPlaceViewHolder, position: Int, model: WorkCard) {
                Picasso.get().load(model.business_logo).fit().centerCrop().placeholder(R.drawable.background_icon).into(holder.binding.logoImageView)
                holder.binding.positionTextView.text = model.user_position
                holder.binding.businessNameTextView.text = model.business_name
                val isAdmin = model.admin
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
                holder.binding.root.setOnClickListener {
                    if (isAdmin == true){
                        val intent = Intent(it.context, AdminBusinessProfileActivity::class.java)
                        intent.putExtra("business_id", model.business_id)
                        startActivity(intent)
                        Animatoo.animateFade(it.context)
                    }else{
                        val intent = Intent(it.context, BusinessDetailsActivity::class.java)
                        intent.putExtra("business_id", model.business_id)
                        startActivity(intent)
                        Animatoo.animateFade(it.context)
                    }
                }
            }

        }
        workPlaceRecycler.adapter = workPlaceAdapter
        workPlaceAdapter.startListening()
    }

    class MyWorkPlaceViewHolder(val binding: WorkCardBinding) : RecyclerView.ViewHolder(binding.root)

    private fun getCardRequest() {
        val query: Query = FirebaseFirestore.getInstance()
            .collection("users").document(constants.currentUserId.toString())
            .collection("card_requests")

        val options: FirestoreRecyclerOptions<RequestCard> = FirestoreRecyclerOptions.Builder<RequestCard>()
            .setQuery(query, RequestCard::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<RequestCard, CardRequestViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardRequestViewHolder {
                val binding = RequestCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return CardRequestViewHolder(binding)
            }

            override fun getItemCount(): Int {
                if(snapshots.size == 0){
                    binding.textView7.visibility = View.GONE
                }else {binding.textView7.visibility = View.VISIBLE}
                return snapshots.size
            }

            override fun onBindViewHolder(holder: CardRequestViewHolder, position: Int, model: RequestCard) {
                Picasso.get().load(model.sender_image).fit().centerCrop().into(holder.binding.senderImageView)
                holder.binding.senderNameTextView.text = model.sender_name
                holder.binding.senderProfessionTextView.text = model.sender_profession
                holder.binding.root.setOnClickListener {
                    val intent = Intent(it.context, UserRequestDetailsActivity::class.java)
                    intent.putExtra("user_id", model.sender_id)
                    startActivity(intent)
                    Animatoo.animateFade(it.context)
                }
            }

        }
        requestRecycler.adapter = adapter
        adapter.startListening()

    }

    class CardRequestViewHolder(val binding: RequestCardBinding) : RecyclerView.ViewHolder(binding.root)

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && resultCode == Activity.RESULT_OK && data != null) {
            if (result!!.contents == null) {
                //Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                businessId = result!!.contents.toString().trim { it <= ' ' }
                val intent = Intent(this, ExistingBusinessActivity::class.java)
                intent.putExtra("business_id", businessId)
                startActivity(intent)
                Animatoo.animateFade(this)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == BUSINESS_REQUEST_GALLERY_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(selectedImage!!)
                var bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.e("TAG", "uri is not a bitmap,$selectedImage")
                    return
                }
                val width: Int = bitmap.width
                val height: Int = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()
                bitmap = null
                val source = RGBLuminanceSource(width, height, pixels)
                val bBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                try {
                    val result: Result = reader.decode(bBitmap)
                    businessId = result.text.toString().trim { it <= ' ' }
                    val intent = Intent(this, ExistingBusinessActivity::class.java)
                    intent.putExtra("business_id", businessId)
                    startActivity(intent)
                    //Animatoo.animateFade(this@HomeActivity2)
                } catch (e: NotFoundException) {
                    // Toast.makeText(this, "This Code is NOT VALID", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "decode exception", e)
                }
            } catch (e: FileNotFoundException) {
                //Log.e("TAG", "can not open file" + selectedImage.toString(), e);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            //Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    companion object {
        const val REQUEST_GALLERY_PHOTO = 8
        const val BUSINESS_REQUEST_GALLERY_PHOTO = 20
    }
}