package com.kumir.settingupdevices

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.kumir.settingupdevices.usb.UsbFragment

class MethodFragments {
    fun getLastFragmentUsb(fragmentManager: FragmentManager): Fragment? {
        // Получаем список всех фрагментов, которые добавлены в FragmentManager
        val fragments = fragmentManager.fragments

        // Ищем последний добавленный фрагмент, который видим и не null
        for (i in fragments.size - 1 downTo 0) {
            val fragment = fragments[i]
            if (fragment != null && fragment.isVisible && fragment is UsbFragment) {
                return fragment
            }
        }
        return null
    }

}