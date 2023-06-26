package io.pridetechnologies.businesscard.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.instantsearch.android.list.autoScrollToStart
import com.algolia.instantsearch.android.paging3.liveData
import com.algolia.instantsearch.android.searchbox.SearchBoxViewAppCompat
import com.algolia.instantsearch.android.stats.StatsTextView
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.connectView
import com.algolia.instantsearch.stats.DefaultStatsPresenter
import com.algolia.instantsearch.stats.connectView
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.SearchResultAdapter
import io.pridetechnologies.businesscard.SearchViewModel
import io.pridetechnologies.businesscard.databinding.FragmentAddBioBinding
import io.pridetechnologies.businesscard.databinding.FragmentSearchBinding


class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by activityViewModels()
    private val connection = ConnectionHandler()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(layoutInflater, container, false)

        val adapterProduct = SearchResultAdapter()
        viewModel.paginator.liveData.observe(viewLifecycleOwner) { pagingData ->
            adapterProduct.submitData(lifecycle, pagingData)
            val TAG = "Algoria Search"
            Log.d(TAG, "Results: $pagingData")
        }
        binding.productList.let {
            it.itemAnimator = null
            it.adapter = adapterProduct
            it.layoutManager = LinearLayoutManager(requireContext())
            it.autoScrollToStart(adapterProduct)
        }
        val searchBoxView = SearchBoxViewAppCompat(binding.searchView)
        connection += viewModel.searchBox.connectView(searchBoxView)
        val statsView = StatsTextView(binding.stats)
        connection += viewModel.stats.connectView(statsView, DefaultStatsPresenter())

        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        connection.clear()
    }

}