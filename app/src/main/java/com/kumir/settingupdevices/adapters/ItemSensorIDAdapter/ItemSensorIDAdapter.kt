package com.kumir.settingupdevices.adapters.ItemPingrecvAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.model.recyclerModel.ItemSensorID


// адаптер для вывода пакетов базовых станций
class ItemSensorIDAdapter (val context: Context,
                           private val list: MutableList<ItemSensorID>
) : RecyclerView.Adapter<ItemSensorIDAdapter.ItemSensorIDHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSensorIDHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_sensor_id,
            parent, false)
        return ItemSensorIDHolder(itemView)
    }

    // обновление данныех
    fun updateItem(position: Int, newItem: ItemSensorID) {
        // исключения выход за пределы массива
        if (position >= getItemCount()) return

        list[position] = newItem
        notifyItemChanged(position)
    }

    fun currentHighlighting(position: Int, newItem: ItemSensorID) {
        // исключения выход за пределы массива
        if (position >= getItemCount()) return


        /*list[position] = newItem
        notifyItemChanged(position)*/
        // ищем свободное место что бы переместить наверх
        val indexFree = exchange()
        if (indexFree != -1) {

            // меняем местами и переотрисывываем
            val freeDoner = list[indexFree]
            list[indexFree] = newItem
            list[position] = freeDoner

            // обновляем позции
            notifyItemChanged(position)
            notifyItemChanged(indexFree)
        }
    }


    // поиск индекса наверху который не найденый и не выделен фиалетовым
    private fun exchange(): Int  {
        var index: Int = 0
        for (i in list) {
            if (i.sensorID.last() != '\n') return index
            index++
        }
        return -1
    }


    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemSensorIDHolder, position: Int) {
        val currentItem = list[position]

        // присваеваем индитифкатор в отображения
        if (currentItem.sensorID.last() == '\n') {
            // случай когда гадо выделить найденый обьект
            holder.textSensorTemp.background = context.getDrawable(R.drawable.rounded_background_current)
            // holder.textSensorID.background = context.getDrawable(R.drawable.rounded_background_current)
            holder.textSensorID.text = currentItem.sensorID.replace("\n", "")
        } else
            holder.textSensorID.text = currentItem.sensorID

        // проверяем какая температура ошибочная это -300 и выводим ее либо ошибку с красным фоном
        if (currentItem.temp != -300F) {
            holder.textSensorTemp.text = currentItem.temp.toString()
            if (currentItem.sensorID.last() != '\n')
                holder.textSensorTemp.background = context.getDrawable(R.drawable.rounded_background_suc)
            // holder.textSensorTemp.textColors = context.getColor(R.color.textColor)
        } else {
            holder.textSensorTemp.text = context.getString(R.string.error)
            if (currentItem.sensorID.last() != '\n')
                holder.textSensorTemp.background = context.getDrawable(R.drawable.error_rounded_background)
        }
    }

    class ItemSensorIDHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSensorID: TextView = itemView.findViewById(R.id.textSensorID)
        val textSensorTemp: TextView = itemView.findViewById(R.id.textSensorTemp)
    }

}