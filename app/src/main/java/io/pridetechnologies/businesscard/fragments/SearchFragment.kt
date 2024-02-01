package io.pridetechnologies.businesscard.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.algolia.instantsearch.android.list.autoScrollToStart
import com.algolia.instantsearch.android.searchbox.SearchBoxViewAppCompat
import com.algolia.instantsearch.android.stats.StatsTextView
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.connectView
import com.algolia.instantsearch.stats.DefaultStatsPresenter
import com.algolia.instantsearch.stats.connectView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.SearchResultAdapter
import io.pridetechnologies.businesscard.SearchViewModel
import io.pridetechnologies.businesscard.databinding.FragmentSearchBinding
import kotlin.properties.Delegates


class SearchFragment : Fragment() {

    private var constants = Constants()
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by activityViewModels()
    private val connection = ConnectionHandler()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude by Delegates.notNull<Double>()
    private var longitude by Delegates.notNull<Double>()
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
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .withListener(object: MultiplePermissionsListener {
                @SuppressLint("MissingPermission")
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if(report.areAllPermissionsGranted()){
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    // Got last known location. In some rare situations, this can be null.
                                    if (location != null) {
                                        // Use the location object to get latitude and longitude
                                        latitude = location.latitude
                                        longitude = location.longitude
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Handle any errors that occurred while trying to get location
                                    constants.showToast(requireContext(), "Error: ${e.message.toString()}")
                                }
                        }
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .withErrorListener {
            }
            .check()

        val searchBoxView = SearchBoxViewAppCompat(binding.searchView)
        val statsView = StatsTextView(binding.stats)
        searchBoxView.onQueryChanged = {
            viewModel.search()
            viewModel.searchResultsWithDistance(requireContext(),latitude, longitude)
                .observe(viewLifecycleOwner) { pagingData ->
                    adapter.submitData(lifecycle, pagingData)
                }
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