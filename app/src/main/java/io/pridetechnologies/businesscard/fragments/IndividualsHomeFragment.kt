package io.pridetechnologies.businesscard.fragments

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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.BusinessDetailsActivity
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.Individuals
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.UserProfileActivity
import io.pridetechnologies.businesscard.WorkCard
import io.pridetechnologies.businesscard.databinding.CustomBioDialogBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import io.pridetechnologies.businesscard.databinding.FragmentIndividualsHomeBinding
import io.pridetechnologies.businesscard.databinding.IndividualsSlidePageBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs


class IndividualsHomeFragment : Fragment() {


    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentIndividualsHomeBinding
    private val constants = Constants()
    private var myExecutor: ExecutorService? = null
    private var myHandler: Handler? = null
    private var myCodeBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentIndividualsHomeBinding.inflate(layoutInflater, container, false)

        myExecutor = Executors.newSingleThreadExecutor()
        myHandler = Handler(Looper.getMainLooper())

        val user = constants.auth.currentUser
        user?.let {
            val photoUrl = it.photoUrl
            Picasso.get().load(photoUrl).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView27)
        }


        binding.imageView27.setOnClickListener {
            val intent = Intent(requireContext(), UserProfileActivity::class.java)
            startActivity(intent)
            Animatoo.animateFade(requireContext())
        }

        binding.cardListRecycler.clipToPadding = false
        binding.cardListRecycler.clipChildren = false
        binding.cardListRecycler.offscreenPageLimit = 3
        binding.cardListRecycler.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(4))
        compositePageTransformer.addTransformer { page, position ->
            val v = 1 - abs(position)
            page.scaleY = 0.8f + v * 0.2f
        }

        binding.cardListRecycler.setPageTransformer(compositePageTransformer)

        retrieveCards()

        return binding.root
    }

    private fun retrieveCards() {
        val query: Query = constants.db.collection("users").document(constants.currentUserId.toString())
            .collection("individuals_cards")

        val options: FirestoreRecyclerOptions<Individuals> = FirestoreRecyclerOptions.Builder<Individuals>()
            .setQuery(query, Individuals::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Individuals, IndividualsViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndividualsViewHolder {
                val binding = IndividualsSlidePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return IndividualsViewHolder(binding)
            }

            override fun onBindViewHolder(holder: IndividualsViewHolder, position: Int, model: Individuals) {
                Picasso.get().load(model.user_image).fit().centerCrop().placeholder(R.mipmap.user_gold).into(holder.binding.circleImageView)
                holder.binding.textView.text = model.user_name

                holder.binding.deleteImageButton.setOnClickListener {

                    val dialog = Dialog(context!!)
                    val b = CustomDialogBoxBinding.inflate(layoutInflater)
                    dialog.setContentView(b.root)
                    b.titleTextView.text = "Delete Card"
                    b.descTextView.text = "Are you sure you want to delete this card?"
                    b.positiveTextView.text = "Delete"
                    b.positiveTextView.setOnClickListener {
                        constants.db.collection("users").document(constants.currentUserId.toString())
                            .collection("individuals_cards").document(model.user_id.toString()).delete()
                            .addOnCompleteListener { dialog.dismiss() }
                    }
                    b.negativeTextView.setOnClickListener { dialog.dismiss() }
                    dialog.setCancelable(false)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }

                constants.db.collection("users").document(model.user_id.toString())
                    .addSnapshotListener { snapshot, e ->

                        if (e != null) {
                            Log.w(ContentValues.TAG, "Listen failed.", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val firstName = snapshot.get("first_name").toString()
                            val userBio = snapshot.get("user_bio").toString()
                            val userMobile = snapshot.get("user_mobile").toString()
                            val userEmail = snapshot.get("email").toString()
                            val qrCode = snapshot.get("user_qr_code").toString()
                            val userLink = snapshot.get("user_link").toString()

                            val profession = snapshot.get("profession").toString()
                            holder.binding.textView2.text = profession

                            holder.binding.bioButton.setOnClickListener {
                                val dialog = Dialog(context!!)
                                val b = CustomBioDialogBinding.inflate(layoutInflater)
                                dialog.setContentView(b.root)
                                b.titleTextView.text = firstName + "'s Bio"
                                b.bioTextView.text = userBio
                                b.closeButton.setOnClickListener { dialog.dismiss() }
                                dialog.setCancelable(true)
                                dialog.window?.setBackgroundDrawable(
                                    ColorDrawable(
                                        Color.TRANSPARENT
                                    )
                                )
                                dialog.show()
                            }

                            holder.binding.callButton.setOnClickListener {
                                if (!userMobile.equals("null")) {
                                    val number = String.format("tel: %s", userMobile)
                                    val callIntent = Intent(Intent.ACTION_CALL)
                                    callIntent.setData(Uri.parse(number))
                                    Dexter.withContext(context)
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
                                            Toast.makeText(context!!.applicationContext, "Error occurred! ", Toast.LENGTH_SHORT).show()
                                        }
                                        .onSameThread()
                                        .check()
                                } else {
                                    Toast.makeText(context, "No Mobile Number", Toast.LENGTH_LONG).show()
                                }
                            }
                            holder.binding.messageButton.setOnClickListener {

                            }
                            holder.binding.emailButton.setOnClickListener {
                                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$userEmail"))
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                                emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                                startActivity(Intent.createChooser(emailIntent, "SEND_MAIL"))
                            }
                            holder.binding.shareButton.setOnClickListener {

                                val dialog = Dialog(context!!)
                                val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                                dialog.setContentView(b.root)
                                b.textView29.text = firstName + "'s" + " Card"
                                Picasso.get().load(qrCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                                b.downloadButton.setOnClickListener {
                                    myExecutor?.execute {
                                        myCodeBitmap = constants.downloadCode(requireContext(), qrCode)
                                        myHandler?.post {
                                            if(myCodeBitmap!=null){
                                                constants.saveMediaToStorage(requireContext(), myCodeBitmap, "${firstName}Code")
                                            }
                                        }
                                    }
                                }
                                b.copyLinkButton.setOnClickListener {
                                    constants.copyText(requireContext(), userLink)
                                }
                                b.button10.setOnClickListener { dialog.dismiss() }
                                dialog.setCancelable(true)
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.show()
                            }

                        } else {
                            Log.d(ContentValues.TAG, "Current data: null")
                        }

                    }

                constants.db.collection("social_media").document(model.user_id.toString())
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
                                holder.binding.facebookBtn.visibility =View.GONE
                            }else holder.binding.facebookBtn.visibility =View.VISIBLE
                            if (whatsAppLink.equals("null") || whatsAppLink.isEmpty()){
                                holder.binding.whatsAppBtn.visibility =View.GONE
                            }else holder.binding.whatsAppBtn.visibility =View.VISIBLE
                            if (linkedInLink.equals("null") || linkedInLink.isEmpty()){
                                holder.binding.linkedInBtn.visibility =View.GONE
                            }else holder.binding.linkedInBtn.visibility =View.VISIBLE
                            if (twitterLink.equals("null") || twitterLink.isEmpty()){
                                holder.binding.twitterBtn.visibility =View.GONE
                            }else holder.binding.twitterBtn.visibility =View.VISIBLE
                            if (youtubeLink.equals("null") || youtubeLink.isEmpty()){
                                holder.binding.youtubeBtn.visibility =View.GONE
                            }else holder.binding.youtubeBtn.visibility =View.VISIBLE
                            if (instagramLink.equals("null") || instagramLink.isEmpty()){
                                holder.binding.instagramBtn.visibility =View.GONE
                            }else holder.binding.instagramBtn.visibility =View.VISIBLE
                            if (weChatLink.equals("null") || weChatLink.isEmpty()){
                                holder.binding.wechatBtn.visibility =View.GONE
                            }else holder.binding.wechatBtn.visibility =View.VISIBLE
                            if (tiktokLink.equals("null") || tiktokLink.isEmpty()){
                                holder.binding.tiktokBtn.visibility =View.GONE
                            }else holder.binding.tiktokBtn.visibility =View.VISIBLE

                            holder.binding.facebookBtn.setOnClickListener {
                                constants.openProfileInApp(requireContext(), "com.facebook.katana",facebookLink)
                            }
                            holder.binding.whatsAppBtn.setOnClickListener{
                                constants.openNumberInWhatsApp(requireContext(),"com.whatsapp",whatsAppLink )
                            }
                            holder.binding.linkedInBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(),"com.linkedin.android",linkedInLink )
                            }
                            holder.binding.twitterBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(),"com.twitter.android",twitterLink )
                            }
                            holder.binding.tiktokBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(),"com.zhiliaoapp.musically",tiktokLink )
                            }
                            holder.binding.wechatBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(),"com.tencent.mm",weChatLink )
                            }
                            holder.binding.instagramBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(),"com.whatsapp",instagramLink )
                            }
                            holder.binding.youtubeBtn.setOnClickListener{
                                constants.openProfileInApp(requireContext(), "com.google.android.youtube",youtubeLink )
                            }
                        } else {
                            Log.d(ContentValues.TAG, "Current data: null")
                        }

                    }

                val verticalLayoutManager = LinearLayoutManager(requireContext())
                val workPlaceRecycler = holder.binding.myWorkRecyclerView
                workPlaceRecycler.layoutManager = verticalLayoutManager

                val workPlaceQuery: Query = FirebaseFirestore.getInstance()
                    .collection("users").document(model.user_id.toString())
                    .collection("my_work_place")
                    .whereEqualTo("is_accepted", true)

                val workPlaceOptions: FirestoreRecyclerOptions<WorkCard> = FirestoreRecyclerOptions.Builder<WorkCard>()
                    .setQuery(workPlaceQuery, WorkCard::class.java)
                    .build()

                val workPlaceAdapter = object : FirestoreRecyclerAdapter<WorkCard, UserProfileActivity.MyWorkPlaceViewHolder>(workPlaceOptions){
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileActivity.MyWorkPlaceViewHolder {
                        val binding = WorkCardBinding
                            .inflate(LayoutInflater.from(parent.context), parent, false)
                        return UserProfileActivity.MyWorkPlaceViewHolder(binding)
                    }

                    override fun getItemCount(): Int {
                        if(snapshots.size == 0){
                            holder.binding.cardView.visibility = View.VISIBLE
                        }else {holder.binding.cardView.visibility = View.GONE}
                        return snapshots.size
                    }

                    override fun onBindViewHolder(holder: UserProfileActivity.MyWorkPlaceViewHolder, position: Int, model: WorkCard) {
                        Picasso.get().load(model.business_logo).fit().centerCrop().placeholder(io.pridetechnologies.businesscard.R.drawable.background_icon).into(holder.binding.logoImageView)
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
                workPlaceRecycler.adapter = workPlaceAdapter
                workPlaceAdapter.startListening()
            }

        }
        binding.cardListRecycler.adapter = adapter
        adapter.startListening()
    }
    class IndividualsViewHolder(val binding: IndividualsSlidePageBinding) : RecyclerView.ViewHolder(binding.root)

}