package com.kumir.settingupdevices.adapters.ItemSensorPipeBlockageAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.model.recyclerModel.ItemSensorPipeBlockage


// адаптер для вывода пакетов базовых станций
class ItemSensorPipeBlockageAdapter (val context: Context,
                           private val list: MutableList<ItemSensorPipeBlockage>
) : RecyclerView.Adapter<ItemSensorPipeBlockageAdapter.ItemSensorPipeBlockageHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSensorPipeBlockageHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_sensor_pipe_blockage,
            parent, false)
        return ItemSensorPipeBlockageHolder(itemView)
    }


    override fun getItemCount(): Int = list.size

    // выводим данные
    override fun onBindViewHolder(holder: ItemSensorPipeBlockageHolder, position: Int) {
        val currentItem = list[position]
        val threshold: String = context.getString(R.string.threshold) + ": " + currentItem.threshold
        val rawValue: String = context.getString(R.string.rawvalue) + ": " + currentItem.rawValue

        val rawState: String = context.getString(R.string.rawstate) + ": " +
                context.getString(
                    if (currentItem.rawState) R.string.Yes
                    else R.string.No
                )
        val state: String = context.getString(R.string.state) + ": " +
                context.getString(
                    if (currentItem.state) R.string.Yes
                    else R.string.No
                )


        if (currentItem.rawState || currentItem.state) {
            // изменение фона на ошибку
            holder.mainLayout.setBackgroundResource(R.drawable.error_rounded_background)
        } else {
            // изменение фона на все зорошо
            holder.mainLayout.setBackgroundResource(R.drawable.rounded_background2)
        }




        holder.threshold.text = threshold
        holder.rawValue.text = rawValue
        holder.rawState.text = rawState
        holder.state.text = state
        holder.address.text = currentItem.address
    }

    class ItemSensorPipeBlockageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mainLayout: ConstraintLayout = itemView.findViewById(R.id.main)
        val threshold: TextView = itemView.findViewById(R.id.textThresholdItem)
        val rawValue: TextView = itemView.findViewById(R.id.textRawValueItem)
        val rawState: TextView = itemView.findViewById(R.id.textRawStateItem)
        val state: TextView = itemView.findViewById(R.id.textStateItem)
        val address: TextView = itemView.findViewById(R.id.textAddress)
    }

}