package com.example.temperaturemeasurement

import android.os.Bundle
import android.view.*
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.example.temperaturemeasurement.databinding.FragmentAboutBinding
import androidx.navigation.fragment.findNavController

class AboutFragment : Fragment() {
    lateinit var binding: FragmentAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.about_settings, menu)

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.about) {
            // Log.e("error", findNavController().currentDestination?.id.toString())
            findNavController().navigate(R.id.action_aboutFragment_to_temperaturesDisplayFragment)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        val view = binding.root

        return view
    }

}