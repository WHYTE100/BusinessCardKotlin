package io.pridetechnologies.businesscard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.algolia.instantsearch.android.inflate
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import io.pridetechnologies.businesscard.activities.NewBusinessCardActivity

class SearchResultAdapter(val context: Context) : PagingDataAdapter<BusinessSearch, SearchResultAdapter.SearchResultViewHolder>(ProductDiffUtil) {

    //var onItemClick: ((String) -> Unit)? = null
    private val constants = Constants()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(parent.inflate(R.layout.search_item))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
            //onItemClick?.invoke(it.objectID)
        }
    }

    object ProductDiffUtil : DiffUtil.ItemCallback<BusinessSearch>() {
        override fun areItemsTheSame(oldItem: BusinessSearch, newItem: BusinessSearch) = oldItem.objectID == newItem.objectID
        override fun areContentsTheSame(oldItem: BusinessSearch, newItem: BusinessSearch) = oldItem == newItem
    }

    inner class SearchResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val businessLogo = view.findViewById<CircleImageView>(R.id.businessLogoView)
        private val businessName = view.findViewById<TextView>(R.id.businessNameView)
        private val businessLocation = view.findViewById<TextView>(R.id.businessAddressView)
        private val businessDistance = view.findViewById<TextView>(R.id.distanceView)
        private val cardView = view.findViewById<MaterialCardView>(R.id.cardView)

        @SuppressLint("SetTextI18n")
        fun bind(businessSearch: BusinessSearch) {
            Picasso.get().load(businessSearch.business_logo).fit().centerCrop().placeholder(R.drawable.background_icon).into(businessLogo)
            businessName.text = businessSearch.business_name
            businessLocation.text = "${businessSearch.area_located}, ${businessSearch.district_name}, ${businessSearch.country}"
            val distanceUnit = constants.getDistanceUnit(context)
            val convertToUnit = when (distanceUnit) {
                "miles" -> {
                    "miles"
                }

                "kilometers" -> {
                    "km"
                }

                else -> {
                    "km"
                }
            }
            businessDistance.text = "${businessSearch.distance} $convertToUnit"


            cardView.setOnClickListener {
                val intent = Intent(context, NewBusinessCardActivity::class.java)
                intent.putExtra("business_id", businessSearch.objectID)
                context.startActivity(intent)
                Animatoo.animateFade(context)
            }
        }

    }
}


