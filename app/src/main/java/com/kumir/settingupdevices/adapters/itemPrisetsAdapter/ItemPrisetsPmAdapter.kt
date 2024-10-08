package com.kumir.settingupdevices.adapters.itemPrisetsAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.dataBasePreset.Pm
import com.kumir.settingupdevices.model.recyclerModel.ItemPresetsPmView
import com.kumir.settingupdevices.settings.PrisetsPmValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment

// адаптер для отображения списка присетов для настроек
class ItemPrisetsPmAdapter(
    private val context: Context,
    private val contextPriset: PrisetFragment<Pm>,
    private val list: List<ItemPresetsPmView>
) : RecyclerView.Adapter<ItemPrisetsPmAdapter.ItemPrisetsPmViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPrisetsPmViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_usb_con,
            parent, false)
        return ItemPrisetsPmViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemPrisetsPmViewHolder, position: Int) {
        val currentItem = list[position]
        currentItem.name.let {name ->
            holder.textName.text = name

            val priset: Pm? = PrisetsPmValue.presets[name]

            // через contextPriset будет выводить при нажатии все данные в поля
            priset.let {
                holder.textName.setOnClickListener {
                    contextPriset.printPriset(priset!!)
                }
            }
        }
    }

    class ItemPrisetsPmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.nameUsb)
    }
}