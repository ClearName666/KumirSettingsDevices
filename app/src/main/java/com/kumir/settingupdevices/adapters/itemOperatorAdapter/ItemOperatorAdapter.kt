package com.kumir.settingupdevices.adapters.itemOperatorAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.model.recyclerModel.ItemOperator

// найденые операторы
class ItemOperatorAdapter ( val context: Context,
    private val list: List<ItemOperator>
    ) : RecyclerView.Adapter<ItemOperatorAdapter.ItemOperatorViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemOperatorViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_operator,
            parent, false)
        return ItemOperatorViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemOperatorViewHolder, position: Int) {
        // получение элемента
        val currentItem = list[position]

        currentItem.let {
            holder.textOperator.text = context.getString(R.string.operatorTitle) + currentItem.operator
            holder.textRxlev.text = context.getString(R.string.rxlevTitle) + currentItem.rxlev
            holder.textArfcn.text = context.getString(R.string.arfcnTitle) + currentItem.arfnc

            if (currentItem.mcc.isNotEmpty()) {
                holder.textMCC.text = context.getString(R.string.mccTitle) + currentItem.mcc
                holder.textMNC.text = context.getString(R.string.mncTitle) + currentItem.mnc

                // попытка перевести в 16 ричный код некоторых данных
                try {
                    holder.textCellid.text = context.getString(R.string.cellidTitle) + currentItem.cellid.toInt(16).toString()
                } catch (e: Exception) {
                    holder.textCellid.text = context.getString(R.string.cellidTitle)
                }
                try {
                    holder.textLac.text = context.getString(R.string.lacTitle) + currentItem.lac.toInt(16).toString()
                } catch (e: Exception) {
                    holder.textLac.text = context.getString(R.string.lacTitle)
                }

                holder.textBsic.text = context.getString(R.string.bsicTitle) + currentItem.bsic.toString()

            } else { // включен краткий режим
                holder.textMCC.visibility = View.GONE
                holder.textMNC.visibility = View.GONE
                holder.textCellid.visibility = View.GONE
                holder.textLac.visibility = View.GONE
                holder.textBsic.visibility = View.GONE
            }


            // отображения картиночки
            if (currentItem.operator.contains("MegaFon")) {
                holder.image.setBackgroundResource(R.drawable.megafon_logo_wine)
            } else if (currentItem.operator.contains("MOTIV")) {
                holder.image.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            } else if (currentItem.operator.contains("MTS")) {
                holder.image.setBackgroundResource(R.drawable.mts__network_provider__logo_wine)
            } else if (currentItem.operator.contains("Bee Line GSM")) {
                holder.image.setBackgroundResource(R.drawable.beeline_seeklogo)
            } else if (currentItem.operator.contains("ROSTELECOM")) {
                holder.image.setBackgroundResource(R.drawable.rostelecom)
            } else {
                holder.image.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            }
        }

    }

    class ItemOperatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textOperator: TextView = itemView.findViewById(R.id.textOperator)
        val textMCC: TextView = itemView.findViewById(R.id.textMCC)
        val textMNC: TextView = itemView.findViewById(R.id.textMNC)
        val textRxlev: TextView = itemView.findViewById(R.id.textRxlev)
        val textCellid: TextView = itemView.findViewById(R.id.textCellid)
        val textArfcn: TextView = itemView.findViewById(R.id.textArfcn)
        val textLac: TextView = itemView.findViewById(R.id.textLac)
        val textBsic: TextView = itemView.findViewById(R.id.textBsic)
        val image: View = itemView.findViewById(R.id.divider)
    }
}