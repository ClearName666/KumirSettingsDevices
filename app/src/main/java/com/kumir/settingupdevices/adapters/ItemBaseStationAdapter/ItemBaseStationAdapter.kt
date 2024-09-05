package com.kumir.settingupdevices.adapters.ItemBaseStationAdapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.model.recyclerModel.ItemBaseStation


// адаптер для вывода базовых станций
class ItemBaseStationAdapter (val context: Context,
                              private val list: List<ItemBaseStation>
) : RecyclerView.Adapter<ItemBaseStationAdapter.ItemBaseStationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBaseStationViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_base_station,
            parent, false)
        return ItemBaseStationViewHolder(itemView)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemBaseStationViewHolder, position: Int) {
        val currentItem = list[position]

        currentItem.let {
            if (currentItem.main) { // если главная базовая станция то перекрашиваем в зеленый
                holder.textBSS.text = context.getString(R.string.BSS) + " " + currentItem.bss
                holder.textSNR.text = context.getString(R.string.SNR) + " " + currentItem.snr

                val sucColor = ContextCompat.getColor(context, R.color.sucColor)
                holder.main.background.setColorFilter(sucColor, PorterDuff.Mode.SRC_ATOP)
            } else {
                holder.textBSS.text = context.getString(R.string.BSS) + " " + currentItem.bss
                holder.textSNR.text = context.getString(R.string.SNR) + " " + currentItem.snr
            }

        }
    }

    class ItemBaseStationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textBSS: TextView = itemView.findViewById(R.id.textBSS)
        val textSNR: TextView = itemView.findViewById(R.id.textSNR)
        val main: ConstraintLayout = itemView.findViewById(R.id.main)
    }

}