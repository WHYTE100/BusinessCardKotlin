package io.pridetechnologies.businesscard

import androidx.lifecycle.ViewModel
import androidx.paging.PagingConfig
import com.algolia.instantsearch.android.paging3.Paginator
import com.algolia.instantsearch.android.paging3.searchbox.connectPaginator
import com.algolia.instantsearch.core.connection.ConnectionHandler
import com.algolia.instantsearch.searchbox.SearchBoxConnector
import com.algolia.instantsearch.searcher.hits.HitsSearcher
import com.algolia.instantsearch.stats.StatsConnector
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName

class SearchViewModel : ViewModel(){

    val searcher = HitsSearcher(
        applicationID = ApplicationID("IKHMA0Z125"),
        apiKey = APIKey("ca743676215813cce0a373a6a16b9db8"),
        indexName = IndexName("businesses")
    )

    val paginator = Paginator(
        searcher = searcher,
        pagingConfig = PagingConfig(pageSize = 20, enablePlaceholders = false),
        transformer = { hit -> hit.deserialize(BusinessSearch.serializer()) }
    )
    val searchBox = SearchBoxConnector(searcher)
    val stats = StatsConnector(searcher)

    private val connection = ConnectionHandler(searchBox, stats)

    init {
        connection += searchBox.connectPaginator(paginator)
    }

    override fun onCleared() {
        super.onCleared()
        searcher.cancel()
    }

}