package com.example.kumirsettingupdevices.adapters.ItemPingrecvAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPingrecv


// адаптер для вывода пакетов базовых станций
class ItemPingrecvAdapter (val context: Context,
                              private val list: List<ItemPingrecv>
) : RecyclerView.Adapter<ItemPingrecvAdapter.ItemBasePingrecvHolder>() {

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
            holder.textSignal.text = context.getString(R.string.signelPackg) + currentItem.signal
            holder.textnumPackg.text = context.getString(R.string.numPackg) + currentItem.number
            holder.texttimeOutPackg.text = context.getString(R.string.timeOutPackg) + currentItem.timeout
        }
    }

    class ItemBasePingrecvHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val texttimeOutPackg: TextView = itemView.findViewById(R.id.texttimeOutPackg)
        val textnumPackg: TextView = itemView.findViewById(R.id.textnumPackg)
        val textSignal: TextView = itemView.findViewById(R.id.textSignal)
    }

}