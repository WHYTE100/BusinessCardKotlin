package io.pridetechnologies.businesscard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.activities.AddAnotherBusinessActivity
import io.pridetechnologies.businesscard.activities.AdminBusinessProfileActivity
import io.pridetechnologies.businesscard.activities.NewBusinessActivity
import io.pridetechnologies.businesscard.databinding.ActivityBusinessDetailsBinding
import io.pridetechnologies.businesscard.databinding.ActivityMyBusinessesBinding
import io.pridetechnologies.businesscard.databinding.WorkCardBinding

class MyBusinessesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBusinessesBinding
    private val constants = Constants()
    private lateinit var workPlaceRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBusinessesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }
        binding.addButton.setOnClickListener {
            addBusiness()
        }
        binding.addButton2.setOnClickListener {
            addBusiness()
        }
        val verticalLayoutManager = LinearLayoutManager(this)
        workPlaceRecycler = binding.workPlaceRecycler
        workPlaceRecycler.layoutManager = verticalLayoutManager

        getMultipleWorkPlace()
    }

    private fun addBusiness() {
        val intent = Intent(this, AddAnotherBusinessActivity::class.java)
        startActivity(intent)
        Animatoo.animateFade(this)
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
                val binding = WorkCardBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return MyWorkPlaceViewHolder(binding)
            }

            override fun getItemCount(): Int {
                if(snapshots.size == 0){
                    binding.addButton2.visibility = View.GONE
                }else {binding.addButton2.visibility = View.VISIBLE}
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

}