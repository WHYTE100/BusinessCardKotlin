package io.pridetechnologies.businesscard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import io.pridetechnologies.businesscard.databinding.ActivityBusinessSearchBinding
import io.pridetechnologies.businesscard.fragments.SearchFragment


class BusinessSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusinessSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProductFragment()
    }

    private fun showProductFragment() {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.container, SearchFragment())
        fragmentTransaction.commit()
    }
}