package io.pridetechnologies.businesscard.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.instantsearch.android.list.autoScrollToStart
import com.algolia.instantsearch.android.paging3.liveData
import com.algolia.instantsearch.android.searchbox.SearchBoxViewAppCompat
import com.algolia.instantsearch.android.stats.StatsTextView
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.connectView
import com.algolia.instantsearch.stats.DefaultStatsPresenter
import com.algolia.instantsearch.stats.connectView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.SearchResultAdapter
import io.pridetechnologies.businesscard.SearchViewModel
import io.pridetechnologies.businesscard.databinding.FragmentSearchBinding


class SearchFragment : Fragment() {

    private var constants = Constants()
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by activityViewModels()
    private val connection = ConnectionHandler()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_PERMISSION = 12
    private var latitude: Double? = null
    private var longitude: Double? = null
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
//        viewModel.paginator.liveData.observe(viewLifecycleOwner) { pagingData ->
//            adapter.submitData(lifecycle, pagingData)
//        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Got last known location. In some rare situations, this can be null.
                    if (location != null) {
                        // Use the location object to get latitude and longitude
                        viewModel.searchResultsWithDistance(location.latitude, location.longitude)
                            .observe(viewLifecycleOwner) { pagingData ->
                                adapter.submitData(lifecycle, pagingData)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors that occurred while trying to get location
                    constants.showToast(requireContext(), "Error: ${e.message.toString()}")
                }
        } else {
            // Location permission not granted, request again
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }



        val searchBoxView = SearchBoxViewAppCompat(binding.searchView)
        val statsView = StatsTextView(binding.stats)
        searchBoxView.onQueryChanged = {
            viewModel.search()
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