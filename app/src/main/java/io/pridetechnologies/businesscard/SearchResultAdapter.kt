package io.pridetechnologies.businesscard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.algolia.instantsearch.android.inflate
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

class SearchResultAdapter : PagingDataAdapter<BusinessSearch, SearchResultViewHolder>(ProductDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(parent.inflate(R.layout.search_item))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    object ProductDiffUtil : DiffUtil.ItemCallback<BusinessSearch>() {
        override fun areItemsTheSame(oldItem: BusinessSearch, newItem: BusinessSearch) = oldItem.objectID == newItem.objectID
        override fun areContentsTheSame(oldItem: BusinessSearch, newItem: BusinessSearch) = oldItem == newItem
    }
}

class SearchResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val itemName = view.findViewById<TextView>(R.id.nameTextView)

    fun bind(businessSearch: BusinessSearch) {
        itemName.text = businessSearch.business_name
    }
}
