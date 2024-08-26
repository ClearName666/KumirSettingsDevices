package com.kumir.settingupdevices.adapters.itemPresetSettingsDataAdapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.SettingsFragment
import com.kumir.settingupdevices.dataBasePreset.PresetDao
import com.kumir.settingupdevices.model.recyclerModel.ItemSettingPreset
import com.kumir.settingupdevices.settings.PrisetsValue
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
                settingsFragment.showConfirmationDialog(
                    context,
                    name,
                    { del(name) }
                )
            }

        }
    }

    class ItemPresetSettingsDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textNamePreset)
        val buttonClear: ImageView = itemView.findViewById(R.id.imageClear)
        val buttonEdit: ImageView = itemView.findViewById(R.id.imageEdit)
    }

    // удаление данных
    private fun del(name: String) {
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
