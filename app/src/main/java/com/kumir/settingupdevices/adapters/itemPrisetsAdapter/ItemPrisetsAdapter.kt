package com.kumir.settingupdevices.adapters.itemPrisetsAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.model.recyclerModel.ItemPrisetsView
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.settings.PrisetsValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment

// адаптер для отображения списка присетов для настроек
class ItemPrisetsAdapter(
    private val context: Context,
    private val contextPriset: PrisetFragment<Priset>,
    private val list: List<ItemPrisetsView>
) : RecyclerView.Adapter<ItemPrisetsAdapter.ItemPrisetsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPrisetsViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_usb_con,
            parent, false)
        return ItemPrisetsViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemPrisetsViewHolder, position: Int) {
        val currentItem = list[position]
        currentItem.name.let {name ->
            holder.textName.text = name

            val priset: Priset? = PrisetsValue.prisets[name]

            // через contextPriset будет выводить при нажатии все данные в поля
            priset.let {
                holder.textName.setOnClickListener {
                    contextPriset.printPriset(priset!!)
                }
            }
        }
    }

    class ItemPrisetsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.nameUsb)
    }
}