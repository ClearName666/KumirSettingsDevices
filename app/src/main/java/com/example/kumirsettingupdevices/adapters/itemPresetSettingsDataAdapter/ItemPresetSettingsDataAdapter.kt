package com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.SettingsFragment
import com.example.kumirsettingupdevices.dataBasePreset.Preset
import com.example.kumirsettingupdevices.dataBasePreset.PresetDao
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSettingPreset
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.settings.PrisetsValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


// адаптер для вывода данных в настроках шаблоны из базы данных
class ItemPresetSettingsDataAdapter(
    private val context: Context,
    private val settingsFragment: SettingsFragment,
    private val presetDao: PresetDao,
    private val list: List<ItemSettingPreset>
) : RecyclerView.Adapter<ItemPresetSettingsDataAdapter.ItemPresetSettingsDataViewHolder>() {



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemPresetSettingsDataViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.preset_item_data_edit,
            parent, false)
        return ItemPresetSettingsDataViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemPresetSettingsDataViewHolder, position: Int) {
        val currentItem = list[position]
        currentItem.name.let { name ->
            holder.textName.text = name


            // изменение записи в базу данных
            holder.buttonEdit.setOnClickListener {
                // получение записи по имени
                CoroutineScope(Dispatchers.IO).launch {
                    val preset = presetDao.getFirstByName(name)
                    (context as Activity).runOnUiThread {
                        settingsFragment.viewEditMenu(preset, null, null)
                    }
                }
                /*if (context is MainActivity) {
                    context.showAlertDialog(context.getString(R.string.noneTypeDevice))
                }*/
            }

            // удаление записи из базы данных
            holder.buttonClear.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    presetDao.deleteByName(name)

                    // удаляем их опертки
                    PrisetsValue.prisets.remove(name)

                    // обновлем адаптер что бы убрать пресет из доступных
                    (context as Activity).runOnUiThread {
                        settingsFragment.updataDataPersetAdapter()
                    }
                }
            }

        }
    }

    class ItemPresetSettingsDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textNamePreset)
        val buttonClear: ImageView = itemView.findViewById(R.id.imageClear)
        val buttonEdit: ImageView = itemView.findViewById(R.id.imageEdit)
    }
}
