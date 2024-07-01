package com.example.kumirsettingupdevices.adapters.ItemAbanentAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemBaseStationAdapter.ItemBaseStationAdapter
import com.example.kumirsettingupdevices.model.recyclerModel.ItemAbanent


class ItemAbanentAdapter (val context: Context,
                                   private val list: List<ItemAbanent>
) : RecyclerView.Adapter<ItemAbanentAdapter.ItemAbanentViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAbanentViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_abanent,
            parent, false)
        return ItemAbanentViewHolder(itemView)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemAbanentViewHolder, position: Int) {
        val item = list[position]
        holder.txtNumAbanent.text = item.num
        holder.textName.text = item.name
        holder.textNumDevice.text = item.numDevice
        holder.textDriver.text = item.driver
        holder.textSettingsRS485.text = "${item.speed}, ${item.parity}, ${item.stopBit}, ${item.bitData}, ${item.timeout}"
        holder.textParams.text = "${item.password}, ${item.adress}, ${item.values}"
    }

    class ItemAbanentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textumAbanent: TextView = itemView.findViewById(R.id.textNumAbanent)
        val txtNumAbanent: TextView = itemView.findViewById(R.id.txtNumAbanent)
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textNumDevice: TextView = itemView.findViewById(R.id.textNumDevice)
        val textDriver: TextView = itemView.findViewById(R.id.textDriver)
        val textSettingsRS485: TextView = itemView.findViewById(R.id.textSettingsRS485)
        val textParams: TextView = itemView.findViewById(R.id.textParams)
    }
}