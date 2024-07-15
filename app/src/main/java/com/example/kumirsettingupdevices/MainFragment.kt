package com.example.kumirsettingupdevices

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentMainBinding


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        /*binding.imageReference.setOnClickListener {
            if (binding.reference.visibility == View.GONE)
                binding.reference.visibility = View.VISIBLE
            else
                binding.reference.visibility = View.GONE
        }*/

        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        binding.imageReference.setOnClickListener {
            if (binding.reference.visibility == View.GONE)
                binding.reference.visibility = View.VISIBLE
            else
                binding.reference.visibility = View.GONE
        }
    }



}