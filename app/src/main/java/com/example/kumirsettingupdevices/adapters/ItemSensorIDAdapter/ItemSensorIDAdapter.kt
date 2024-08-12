package com.example.kumirsettingupdevices.adapters.ItemPingrecvAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPingrecv
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSensorID


// адаптер для вывода пакетов базовых станций
class ItemSensorIDAdapter (val context: Context,
                           private val list: List<ItemSensorID>
) : RecyclerView.Adapter<ItemSensorIDAdapter.ItemSensorIDHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSensorIDHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_sensor_id,
            parent, false)
        return ItemSensorIDHolder(itemView)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemSensorIDHolder, position: Int) {
        val currentItem = list[position]

        // присваеваем индитифкатор в отображения
        holder.textSensorID.text = currentItem.sensorID

        // проверяем какая температура ошибочная это -300 и выводим ее либо ошибку с красным фоном
        if (currentItem.temp != -300F) {
            holder.textSensorTemp.text = currentItem.temp.toString()
            holder.textSensorTemp.background = context.getDrawable(R.drawable.rounded_background2)
        } else {
            holder.textSensorTemp.text = context.getString(R.string.error)
            holder.textSensorTemp.background = context.getDrawable(R.drawable.error_rounded_background)
        }

    }

    class ItemSensorIDHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSensorID: TextView = itemView.findViewById(R.id.textSensorID)
        val textSensorTemp: TextView = itemView.findViewById(R.id.textSensorTemp)
    }

}