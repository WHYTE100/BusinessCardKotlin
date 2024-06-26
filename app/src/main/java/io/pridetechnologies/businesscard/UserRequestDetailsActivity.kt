package io.pridetechnologies.businesscard

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.databinding.ActivityNewCardBinding
import io.pridetechnologies.businesscard.databinding.ActivityUserRequestDetailsBinding
import io.pridetechnologies.businesscard.databinding.CustomDialogBoxBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding

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
                val otherName = document.getString("other_names")
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
                    //Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!")
                }
                .addOnFailureListener { e ->
                    progressDialog.hide()
                    dialog.dismiss()
                    //Log.w(ContentValues.TAG, "Error writing document", e)
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

}