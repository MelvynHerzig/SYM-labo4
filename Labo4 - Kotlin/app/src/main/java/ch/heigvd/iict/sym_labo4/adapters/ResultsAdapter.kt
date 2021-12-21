package ch.heigvd.iict.sym_labo4.adapters

import android.bluetooth.le.ScanResult
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ch.heigvd.iict.sym_labo4.R

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2019 - HEIG-VD, IICT
 */
class ResultsAdapter(private val context: Context) : BaseAdapter() {

    private val results = mutableListOf<ScanResult>()

    fun clear() {
        results.clear()
        notifyDataSetChanged()
    }

    fun addDevice(newResult: ScanResult?) {
        if (newResult == null) return
        var alreadyInAdapter = false
        for (device in results) {
            if (device.device.address.equals(newResult.device.address, ignoreCase = true)) {
                alreadyInAdapter = true
                break
            }
        }
        if (!alreadyInAdapter) {
            results.add(newResult)
            results.sortWith{ o1: ScanResult, o2: ScanResult -> o1.rssi - o2.rssi }
            notifyDataSetChanged()
        }
    }

    override fun getCount() = results.size

    override fun getItemId(position: Int) = 0L

    override fun hasStableIds() = false

    override fun getItem(position: Int) = results[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.scan_item, parent, false)
        }

        //link to gui
        val name = view!!.findViewById<TextView>(R.id.scan_item_name)
        val address = view.findViewById<TextView>(R.id.scan_item_address)

        //fill gui
        val result = getItem(position)

        var deviceName = result.device.name
        if (deviceName == null || deviceName.trim { it <= ' ' }.isEmpty())
            deviceName = context.getString(android.R.string.unknownName)
        name.text = deviceName
        address.text = result.device.address

        return view
    }

}