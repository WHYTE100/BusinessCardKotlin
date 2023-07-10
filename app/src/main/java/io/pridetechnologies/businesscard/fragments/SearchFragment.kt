package io.pridetechnologies.businesscard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.instantsearch.android.list.autoScrollToStart
import com.algolia.instantsearch.android.paging3.liveData
import com.algolia.instantsearch.android.searchbox.SearchBoxViewAppCompat
import com.algolia.instantsearch.android.stats.StatsTextView
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.connectView
import com.algolia.instantsearch.stats.DefaultStatsPresenter
import com.algolia.instantsearch.stats.connectView
import io.pridetechnologies.businesscard.SearchResultAdapter
import io.pridetechnologies.businesscard.SearchViewModel
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

        binding.backButton.setOnClickListener {
            activity?.finish()
        }
        val adapter = SearchResultAdapter(requireContext())
        viewModel.paginator.liveData.observe(viewLifecycleOwner) { pagingData ->
            adapter.submitData(lifecycle, pagingData)
        }

        val searchBoxView = SearchBoxViewAppCompat(binding.searchView)
        val statsView = StatsTextView(binding.stats)
        searchBoxView.onQueryChanged = {

        }
        connection += viewModel.searchBox.connectView(searchBoxView)
        connection += viewModel.stats.connectView(statsView, DefaultStatsPresenter())
        binding.productList.let { view ->
            view.itemAnimator = null
            view.adapter = adapter
            view.layoutManager = LinearLayoutManager(requireContext())
            view.autoScrollToStart(adapter)
        }
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        connection.clear()
    }

}