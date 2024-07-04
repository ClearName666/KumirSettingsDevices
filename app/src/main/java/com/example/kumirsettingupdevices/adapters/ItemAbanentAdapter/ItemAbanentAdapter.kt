package com.example.kumirsettingupdevices.adapters.ItemAbanentAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.EditDelIntrface
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemBaseStationAdapter.ItemBaseStationAdapter
import com.example.kumirsettingupdevices.model.recyclerModel.ItemAbanent
import com.example.kumirsettingupdevices.usbFragments.P101Fragment


class ItemAbanentAdapter (val context: Context, private val list: List<ItemAbanent>,val editDel: EditDelIntrface<ItemAbanent>
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
        holder.txtNumAbanent.text = context.getString(R.string.numberAbanentTitle) + " " + item.num
        holder.textNumAbanent.text = context.getString(R.string.numberAbanentTitle) + " " + item.num
        holder.textName.text = context.getString(R.string.nameTitle) + " " + item.name
        holder.textNumDevice.text = context.getString(R.string.numDevTitle) + " " + item.numDevice
        holder.textDriver.text = context.getString(R.string.driverTitle) + " " + item.driver
        holder.textSettingsRS485.text = context.getString(R.string.setPorsTitle) + " " + item.port
        holder.textParams.text = context.getString(R.string.paramsTitle) + " " + item.values

        // кнопка изменения
        holder.butEdit.setOnClickListener {
            editDel.edit(item)
        }
        // кнопка удаления
        holder.butDel.setOnClickListener {
            editDel.del(item)
        }
    }

    class ItemAbanentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNumAbanent: TextView = itemView.findViewById(R.id.textNumAbanent)
        val txtNumAbanent: TextView = itemView.findViewById(R.id.txtNumAbanent)
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textNumDevice: TextView = itemView.findViewById(R.id.textNumDevice)
        val textDriver: TextView = itemView.findViewById(R.id.textDriver)
        val textSettingsRS485: TextView = itemView.findViewById(R.id.textSettingsRS485)
        val textParams: TextView = itemView.findViewById(R.id.textParams)
        val butEdit: ImageView = itemView.findViewById(R.id.imageEdit)
        val butDel: ImageView = itemView.findViewById(R.id.imageDel)
    }
}