package io.pridetechnologies.businesscard.activities

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.BusinessDetailsActivity
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.CustomProgressDialog
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.WorkCard
import io.pridetechnologies.businesscard.databinding.ActivityNewCardBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding
import io.pridetechnologies.businesscard.fragments.AddUserDetailsFragmentDirections

class NewCardActivity : AppCompatActivity() {

    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityNewCardBinding
    private val constants = Constants()
    private var photoUrl: String? = ""
    private var userName: String? = ""
    private var userProfession: String? = ""
    private var deepLink: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        binding.requestButton.setOnClickListener {
            sendCardRequest()
        }
        val verticalLayoutManager = LinearLayoutManager(this)
        binding.workPlaceRecycler.layoutManager = verticalLayoutManager

        deepLink = intent.getStringExtra("deepLink")
        //binding.textView6.text = deepLink
        //Log.d(TAG, "User Address: $deepLink")
        //Toast.makeText(this, "UID: $deepLink", Toast.LENGTH_LONG).show()
        constants.db.collection("users").document(deepLink.toString())
            .get()
            .addOnSuccessListener { document ->
                val userFirstName = document.getString("first_name")
                val otherName = document.getString("other_names")
                val userLastName = document.getString("surname")
                userProfession = document.getString("profession")
                val userImage = document.getString("profile_image_url")

                Picasso.get().load(userImage).fit().centerCrop().placeholder(R.mipmap.user_gold).into(binding.imageView2)
                binding.textView6.text = "$userFirstName $userLastName"
                binding.textView7.text = userProfession
            }
            .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e)
            }
        getMultipleWorkPlace()
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

    private fun sendCardRequest() {
        progressDialog.show("Sending Request...")
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
            "request_note" to ""
        )

        constants.db.collection("users").document(deepLink.toString())
            .collection("card_requests").document(constants.currentUserId.toString())
            .set(requestDetails, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.hide()
                binding.requestButton.isEnabled = false
                binding.requestButton.text = "Request Sent"
                Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                progressDialog.hide()
                Log.w(ContentValues.TAG, "Error writing document", e) }

    }
}