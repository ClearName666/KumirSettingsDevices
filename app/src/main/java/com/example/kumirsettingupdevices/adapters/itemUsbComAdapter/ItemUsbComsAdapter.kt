package com.example.kumirsettingupdevices.adapters.itemUsbComAdapter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.model.recyclerModel.ItemUsbComsView
import com.example.kumirsettingupdevices.usb.UsbActivityInterface

// адаптер для отображения списка usbCom портов
class ItemUsbComsAdapter(
    private val context: Context,
    private val list: List<ItemUsbComsView>
) : RecyclerView.Adapter<ItemUsbComsAdapter.ItemUsbComsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemUsbComsViewHolder {
        val itemView: View = LayoutInflater.from(context).inflate(
            R.layout.item_usb_con,
            parent, false)
        return ItemUsbComsViewHolder(itemView)
    }

    override fun getItemCount():Int = list.size

    override fun onBindViewHolder(holder: ItemUsbComsViewHolder, position: Int) {
        val currentItem = list[position]
        currentItem.nameUsb.let {nameUsb ->
            holder.textNameUsbCom.text = nameUsb

            holder.textNameUsbCom.setOnClickListener {
                val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
                val device: UsbDevice? = deviceList[nameUsb]

                try {
                    device.let {
                        if (context is UsbActivityInterface) {
                            context.showDeviceName(nameUsb)
                            context.connectToUsbDevice(device!!)
                        }
                    }
                } catch (e: Exception) {
                    if (context is UsbActivityInterface) {
                        context.withdrawalsShow(context.getString(R.string.mainActivityText_ExtractDevice))
                    }
                }
                // дописать клик по порту
            }
        }
    }

    class ItemUsbComsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNameUsbCom: TextView = itemView.findViewById(R.id.nameUsb)
    }
}