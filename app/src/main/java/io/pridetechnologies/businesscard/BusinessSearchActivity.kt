package io.pridetechnologies.businesscard

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import io.pridetechnologies.businesscard.databinding.ActivityBusinessSearchBinding
import io.pridetechnologies.businesscard.fragments.SearchFragment


class BusinessSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusinessSearchBinding
    private var constants = Constants()
    var list = ArrayList<String>()
    private lateinit var listView: ListView
    private lateinit var adapter: SearchResultAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //listView = binding.listView
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        adapter = SearchResultAdapter()
//        recyclerView.adapter = adapter

//        val client = HitsSearcher(
//
//        )

//        runBlocking {
//            try {
//                val query = Query("pride")
//                val response = client.query.)
//                Log.d(TAG, "Results: $response")
//
//            } catch (e: Exception){
//                e.printStackTrace()
//                Log.d(TAG, "Error:", e)
//            }
//        }
//        runBlocking {
//            try {
//                val query = Query("pride").apply {
//                        attributesToRetrieve
//                        hitsPerPage = 29
//                    }
//                val searchResults = index.search(query)
//                val hits = searchResults.hits
//                Log.d(TAG, "Results: $searchResults")
//                val list: MutableList<String> = ArrayList()
//                for (hit in hits) {
//                    val productName = hit.json["businessname"]
//                    //list.add(productName.toString())
//                }
////                val arrayAdapter =
////                    ArrayAdapter<String>(this@BusinessSearchActivity, R.layout.simple_list_item_1, list)
////                listView.adapter = arrayAdapter
//            } catch (e: JSONException) {
//                e.printStackTrace()
//            } finally {
//            // Don't forget to close the client when you're done
//            client.close()
//        }
//        }

//        binding.editTextText.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (!s.toString().equals("")){
//                    list.clear()
//                    val query = Query(s.toString())
//                    runBlocking {
//                        try {
////                            val searchResults = index.search(query, null)
////                            //Log.d(TAG, "Results: ${searchResults.hits}")
////                            val hits = searchResults.hits
////                            Log.d(TAG, "Results: ${hits}")
////                            val searchResultsList = mutableListOf<JsonObject>()
////                            for (hit in hits) {
////                                Log.d(TAG, "Result: ${hit.json}")
////                                //searchResultsList.add(hit.json)
////                            }
//                            //adapter.setSearchResults(searchResultsList)
//                        }catch (e:Exception){
//                            e.printStackTrace()
//                            Log.d(TAG, "Error:", e)
//                        }
//                    }
//                }else{
//                    list.clear()
//                }
//
//
//            }
//
//        })

//        constants.db.collection("users").document(constants.currentUserId.toString())
//            .get()
//            .addOnSuccessListener { document ->
//                if (document != null) {
//                    list.add(document.get("first_name").toString())
//                    val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list)
//                    binding.listView.adapter = arrayAdapter
//                    //Log.d(TAG, "DocumentSnapshot data: ${document.data}")
//                } else {
//                    Log.d(TAG, "No such document")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.d(TAG, "get failed with ", exception)
//            }

        showProductFragment()
    }

    private fun showProductFragment() {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(io.pridetechnologies.businesscard.R.id.container, SearchFragment())
        fragmentTransaction.commit()
    }
}