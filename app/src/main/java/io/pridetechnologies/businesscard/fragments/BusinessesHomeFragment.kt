package io.pridetechnologies.businesscard.fragments

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
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
import io.pridetechnologies.businesscard.BusinessDetailsActivity
import io.pridetechnologies.businesscard.Businesses
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.Individuals
import io.pridetechnologies.businesscard.MyBusinessesActivity
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.UserProfileActivity
import io.pridetechnologies.businesscard.activities.TeamMemberDetailsActivity
import io.pridetechnologies.businesscard.databinding.BusinessesSlidePageBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import io.pridetechnologies.businesscard.databinding.FragmentBusinessesHomeBinding
import io.pridetechnologies.businesscard.databinding.FragmentIndividualsHomeBinding
import io.pridetechnologies.businesscard.databinding.IndividualsSlidePageBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs


class BusinessesHomeFragment : Fragment() {
    private val progressDialog by lazy { CustomProgressDialog(requireContext()) }
    private lateinit var binding: FragmentBusinessesHomeBinding
    private val constants = Constants()
    private var myExecutor: ExecutorService? = null
    private var myHandler: Handler? = null
    private var myCodeBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBusinessesHomeBinding.inflate(layoutInflater, container, false)

        myExecutor = Executors.newSingleThreadExecutor()
        myHandler = Handler(Looper.getMainLooper())

        binding.imageView27.setOnClickListener {
            val intent = Intent(requireContext(), MyBusinessesActivity::class.java)
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
            .collection("businesses_cards")

        val options: FirestoreRecyclerOptions<Businesses> = FirestoreRecyclerOptions.Builder<Businesses>()
            .setQuery(query, Businesses::class.java)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Businesses, BusinessesViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessesViewHolder {
                val binding = BusinessesSlidePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return BusinessesViewHolder(binding)
            }

            override fun onBindViewHolder(holder: BusinessesViewHolder, position: Int, model: Businesses) {
                Picasso.get().load(model.business_logo).fit().centerCrop().placeholder(R.drawable.background_icon).into(holder.binding.circleImageView7)
                holder.binding.textView50.text = model.business_name

                holder.binding.deleteImageButton.setOnClickListener {

                    val dialog = Dialog(context!!)
                    val b = CustomDialogBoxBinding.inflate(layoutInflater)
                    dialog.setContentView(b.root)
                    b.titleTextView.text = "Delete Card"
                    b.descTextView.text = "Are you sure you want to delete this card?"
                    b.positiveTextView.text = "Delete"
                    b.positiveTextView.setOnClickListener {
                        constants.db.collection("users").document(constants.currentUserId.toString())
                            .collection("businesses_cards").document(model.business_id.toString()).delete()
                            .addOnCompleteListener { dialog.dismiss() }
                    }
                    b.negativeTextView.setOnClickListener { dialog.dismiss() }
                    dialog.setCancelable(false)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                }

                constants.db.collection("businesses").document(model.business_id.toString())
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w(ContentValues.TAG, "Listen failed.", e)
                            return@addSnapshotListener
                        }
                        val businessBio = snapshot?.get("business_bio").toString()
                        val businessMobile = snapshot?.get("business_mobile").toString()
                        val businessWebsite = snapshot?.get("business_website").toString()
                        val businessEmail = snapshot?.get("business_email").toString()
                        val businessCode = snapshot?.get("business_qr_code").toString()
                        val businessLink = snapshot?.get("business_link").toString()

                        holder.binding.textView71.text = businessBio

                        if (businessMobile.equals("null") || businessMobile.isEmpty()){
                            holder.binding.callBtn.visibility = View.GONE
                        }
                        if (businessWebsite.equals("null") || businessWebsite.isEmpty()){
                            holder.binding.bioBtn.visibility = View.GONE
                        }
                        if (businessEmail.equals("null") || businessEmail.isEmpty()){
                            holder.binding.emailBtn.visibility = View.GONE
                        }
                        if (businessBio.equals("null") || businessBio.isEmpty()){
                            holder.binding.textView71.visibility = View.GONE
                        }

                        holder.binding.callButton.setOnClickListener {
                            if (!businessMobile.equals("null")) {
                                val number = String.format("tel: %s", businessMobile)
                                val callIntent = Intent(Intent.ACTION_CALL)
                                callIntent.setData(Uri.parse(number))
                                Dexter.withContext(requireContext())
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
                                        Toast.makeText(requireContext(), "Error occurred! ", Toast.LENGTH_SHORT).show()
                                    }
                                    .onSameThread()
                                    .check()
                            } else {
                                Toast.makeText(requireContext(), "No Mobile Number", Toast.LENGTH_LONG).show()
                            }
                        }
                        holder.binding.whatsAppButton.setOnClickListener {
                            if (!businessMobile.equals("null")) {
                                constants.openNumberInWhatsApp(requireContext(), "com.whatsapp", businessMobile)
                            }
                        }
                        holder.binding.emailButton.setOnClickListener {
                            val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$businessEmail"))
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                            emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                            startActivity(Intent.createChooser(emailIntent, "SEND_MAIL"))
                        }
                        holder.binding.websiteButton.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(businessWebsite))
                            startActivity(intent)
                        }
                        holder.binding.shareButton.setOnClickListener {

                            val dialog = Dialog(requireContext())
                            val b = CustomQrCodeDialogBinding.inflate(layoutInflater)
                            dialog.setContentView(b.root)
                            b.textView29.text = "Business Card"
                            Picasso.get().load(businessCode).fit().centerCrop().placeholder(R.drawable.qr_code_black).into(b.imageView9)
                            b.downloadButton.setOnClickListener {
                                myExecutor?.execute {
                                    myCodeBitmap = constants.downloadCode(requireContext(), businessCode)
                                    myHandler?.post {
                                        if(myCodeBitmap!=null){
                                            constants.saveMediaToStorage(requireContext(), myCodeBitmap, model.business_name.toString())
                                        }
                                    }
                                }
                            }
                            b.copyLinkButton.setOnClickListener {
                                constants.copyText(requireContext(), businessLink)
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

                            holder.binding.textView52.text = buildingNumber
                            holder.binding.textView53.text = streetName
                            holder.binding.textView54.text = areaLocated
                            holder.binding.textView55.text = districtName
                            holder.binding.textView56.text = country
                        } else {
                            Log.d(ContentValues.TAG, "Current data: null")
                        }
                    }

                constants.db.collection("social_media").document(model.business_id.toString())
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

                        if (!model.business_bio.equals("null") || instagramLink.isNotEmpty()){
                            holder.binding.textView70.visibility = View.VISIBLE
                            holder.binding.textView71.visibility = View.VISIBLE
                        }
                        if (facebookLink.equals("null") || facebookLink.isEmpty()){
                            holder.binding.facebookBtn.setColorFilter(R.color.darkPrimaryColor)
                        }else holder.binding.facebookBtn.colorFilter = null
                        if (linkedInLink.equals("null") || linkedInLink.isEmpty()){
                            holder.binding.linkedInBtn.setColorFilter(R.color.darkPrimaryColor)
                        }else holder.binding.linkedInBtn.colorFilter = null
                        if (twitterLink.equals("null") || twitterLink.isEmpty()){
                            holder.binding.twitterBtn.setColorFilter(R.color.darkPrimaryColor)
                        }else holder.binding.twitterBtn.colorFilter = null
                        if (youtubeLink.equals("null") || youtubeLink.isEmpty()){
                            holder.binding.youtubeBtn.setColorFilter(R.color.darkPrimaryColor)
                        }else holder.binding.youtubeBtn.colorFilter = null
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
                            constants.openProfileInApp(requireContext(), "com.facebook.katana", facebookLink)
                        }
                        holder.binding.whatsAppButton.setOnClickListener{
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

                    }

                holder.binding.button32.setOnClickListener {
                    val intent = Intent(requireContext(), BusinessDetailsActivity::class.java)
                    intent.putExtra("business_id", model.business_id)
                    startActivity(intent)
                    Animatoo.animateFade(requireContext())
                }
            }

        }
        binding.cardListRecycler.adapter = adapter
        adapter.startListening()
    }
    class BusinessesViewHolder(val binding: BusinessesSlidePageBinding) : RecyclerView.ViewHolder(binding.root)
}