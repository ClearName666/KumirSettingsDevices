package com.kumir.settingupdevices.adapters.itemPrisetsAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.dataBasePreset.Enfora
import com.kumir.settingupdevices.model.recyclerModel.ItemPresetsEnforaView
import com.kumir.settingupdevices.settings.PresetsEnforaValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment

// адаптер для отображения списка присетов для настроек
class ItemPrisetsEnforaAdapter(
    private val context: Context,
    private val contextPriset: PrisetFragment<Enfora>,
    private val list: List<ItemPresetsEnforaView>
) : RecyclerView.Adapter<ItemPrisetsEnforaAdapter.ItemPrisetsEnforaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPrisetsEnforaViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_usb_con,
            parent, false)
        return ItemPrisetsEnforaViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemPrisetsEnforaViewHolder, position: Int) {
        val currentItem = list[position]
        currentItem.name.let {name ->
            holder.textName.text = name

            val priset: Enfora? = PresetsEnforaValue.presets[name]

            // через contextPriset будет выводить при нажатии все данные в поля
            priset.let {
                holder.textName.setOnClickListener {
                    contextPriset.printPriset(priset!!)
                }
            }
        }
    }

    class ItemPrisetsEnforaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.nameUsb)
    }
}