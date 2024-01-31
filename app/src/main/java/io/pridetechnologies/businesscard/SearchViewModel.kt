package io.pridetechnologies.businesscard

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
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
import com.algolia.search.helper.deserialize
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.client.Index
import com.algolia.search.model.search.Query
import kotlinx.coroutines.flow.Flow

class SearchViewModel : ViewModel(){

    private var searcher = HitsSearcher(
        applicationID = ApplicationID("0MC7C7BCYB"),
        apiKey = APIKey("cc7d37100800f423b90379937359e58f"),
        indexName = IndexName("businesses")
    )

    private var paginator = Paginator(
        searcher = searcher,
        pagingConfig = PagingConfig(pageSize = 20, enablePlaceholders = false),
        transformer = { hit -> hit.deserialize(BusinessSearch.serializer()) }
    )
    val searchBox = SearchBoxConnector(searcher)
    val stats = StatsConnector(searcher)

    private val connection = ConnectionHandler(searchBox, stats)

    fun searchResultsWithDistance(currentLatitude: Double, currentLongitude: Double): LiveData<PagingData<BusinessSearch>> {
        val currentLocation = Location("").apply {
            latitude = currentLatitude
            longitude = currentLongitude
        }

        return paginator.liveData.map { pagingData ->
            pagingData.map { searchResultItem ->
                // Calculate the distance for each SearchResultItem
                val resultLocation = Location("").apply {
                    latitude = searchResultItem.business_latitude
                    longitude = searchResultItem.business_longitude
                }
                val distance = currentLocation.distanceTo(resultLocation)
                searchResultItem.copy(distance = distance)
            }
        }
    }
    fun search() {
    connection += searchBox.connectPaginator(paginator)
//        val query = Query(query = queryText)
//        viewModelScope.launch {
//            val response = searcher.search()
//            _searchResults.value = response?.hits?.deserialize(BusinessSearch.serializer()).filterNotNull()
//        }
    }


    override fun onCleared() {
        super.onCleared()
        searcher.cancel()
        connection.clear()
    }

}