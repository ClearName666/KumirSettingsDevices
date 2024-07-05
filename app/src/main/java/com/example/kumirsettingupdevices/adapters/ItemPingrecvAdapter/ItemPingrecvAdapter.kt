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


// адаптер для вывода пакетов базовых станций
class ItemPingrecvAdapter (val context: Context,
                              private val list: List<ItemPingrecv>
) : RecyclerView.Adapter<ItemPingrecvAdapter.ItemBasePingrecvHolder>() {

    companion object {
        private const val SIGNAL_1: Int = 90
        private const val SIGNAL_2: Int = 80
        private const val SIGNAL_3: Int = 70
        private const val SIGNAL_4: Int = 60
        //private const val SIGNAL_5: Int = 50

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBasePingrecvHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_pingrecv,
            parent, false)
        return ItemBasePingrecvHolder(itemView)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ItemBasePingrecvHolder, position: Int) {
        val currentItem = list[position]

        currentItem.let {
            if (currentItem.signal.isNotEmpty()) {
                holder.textSignal.text = context.getString(R.string.signelPackg) + currentItem.signal
            } else {
                holder.textSignal.text = ""
            }

            holder.textnumPackg.text = context.getString(R.string.numPackg) + " " + currentItem.number
            holder.texttimeOutPackg.text = context.getString(R.string.timeOutPackg) + " " + currentItem.timeout

            // выводим градацию сигнала
            try {
                val signalInt: Int = currentItem.signal.substringAfter("-").substringBefore("dBm").toInt()
                if (signalInt > SIGNAL_1) {
                    holder.imageSignal.setBackgroundResource(R.drawable.signal_1)
                } else if (signalInt > SIGNAL_2) {
                    holder.imageSignal.setBackgroundResource(R.drawable.signal_2)
                } else if (signalInt > SIGNAL_3) {
                    holder.imageSignal.setBackgroundResource(R.drawable.signal_3)
                } else if (signalInt > SIGNAL_4) {
                    holder.imageSignal.setBackgroundResource(R.drawable.signal_4)
                } else {
                    holder.imageSignal.setBackgroundResource(R.drawable.signal_5)
                }

            } catch (_: Exception) {}
        }
    }

    class ItemBasePingrecvHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val texttimeOutPackg: TextView = itemView.findViewById(R.id.texttimeOutPackg)
        val textnumPackg: TextView = itemView.findViewById(R.id.textnumPackg)
        val textSignal: TextView = itemView.findViewById(R.id.textSignal)
        val imageSignal: ImageView = itemView.findViewById(R.id.imageSignal)
    }

}