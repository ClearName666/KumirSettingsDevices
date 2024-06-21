package com.example.kumirsettingupdevices.diag

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentDiagPM81Binding


class DiagPM81Fragment : Fragment() {

    private lateinit var binging: FragmentDiagPM81Binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binging = FragmentDiagPM81Binding.inflate(inflater)
        return binging.root
    }

}