package io.pridetechnologies.businesscard

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.algolia.instantsearch.android.paging3.Paginator
import com.algolia.instantsearch.android.paging3.liveData
import com.algolia.instantsearch.android.paging3.searchbox.connectPaginator
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.SearchBoxConnector
import com.algolia.instantsearch.searcher.hits.HitsSearcher
import com.algolia.instantsearch.stats.StatsConnector
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName

class SearchViewModel() : ViewModel() {
    val constants = Constants()

    private var searcher = HitsSearcher(
        applicationID = ApplicationID("0MC7C7BCYB"),
        apiKey = APIKey("cc7d37100800f423b90379937359e58f"),
        indexName = IndexName("businesses")
    )

    val paginator = Paginator(
        searcher = searcher,
        pagingConfig = PagingConfig(pageSize = 50, enablePlaceholders = false),
        transformer = { hit -> hit.deserialize(BusinessSearch.serializer()) }
    )
    val searchBox = SearchBoxConnector(searcher)
    val stats = StatsConnector(searcher)

    val connection = ConnectionHandler(searchBox, stats)

    init {
        connection += searchBox.connectPaginator(paginator)
    }

//    fun searchResultsWithDistance(context: Context ,currentLatitude: Double, currentLongitude: Double): LiveData<PagingData<BusinessSearch>> {
//        val currentLocation = Location("").apply {
//            latitude = currentLatitude
//            longitude = currentLongitude
//        }
//
//        return paginator.liveData.map { pagingData ->
//            pagingData.map { searchResultItem ->
//                // Calculate the distance for each SearchResultItem
//                val resultLocation = Location("").apply {
//                    latitude = searchResultItem.business_latitude
//                    longitude = searchResultItem.business_longitude
//                }
//                val distance = constants.getNavigationDistance(context, currentLocation, resultLocation, "AIzaSyAxZClYe6QwPkY6tV0bK8egHX7g0yMx59I")
//                searchResultItem.copy(distance = distance)
//            }
//
//        }
//    }
    fun search() {

    }

    override fun onCleared() {
        super.onCleared()
        searcher.cancel()
        connection.clear()
    }


}