package com.excellence.camera.sample

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.excellence.basetoolslibrary.baseadapter.CommonAdapter
import com.excellence.basetoolslibrary.baseadapter.ViewHolder

class DeviceListDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "DeviceList"
    }

    private val usbDevices = ArrayList<UsbDevice>()
    private var mUsbDevice: UsbDevice? = null
    private var mOnDeviceItemSelectListener: OnDeviceItemSelectListener? = null
    private lateinit var mAdapter: CommonAdapter<UsbDevice>

    private lateinit var listView: ListView
    private lateinit var tvEmptyTip: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_device_list, null, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.list_view)
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip)

        initDeviceList()
    }

    private fun initDeviceList() {
        mAdapter = object : CommonAdapter<UsbDevice>(usbDevices, R.layout.fragment_device_item) {

            override fun convert(viewHolder: ViewHolder, item: UsbDevice?, position: Int) {
                viewHolder.setChecked(R.id.rbDeviceSelected, mUsbDevice == item)
                viewHolder.setText(R.id.tvProductName, item?.productName ?: item?.manufacturerName)
                viewHolder.setText(R.id.tvDeviceName, item?.deviceName)
            }

        }

        listView.adapter = mAdapter
        listView.setOnItemClickListener { parent, view, position, id ->
            mOnDeviceItemSelectListener?.onItemSelect(mAdapter.getItem(position))
            dismiss()
        }

    }

    fun isShowing(): Boolean {
        return isAdded
    }

    fun show(manager: FragmentManager, deviceList: List<UsbDevice>, usbDevice: UsbDevice?) {
        if (!isShowing()) {
            super.show(manager, TAG)
        }

        notifySelectDevice(deviceList, usbDevice)
    }

    fun notifySelectDevice(deviceList: List<UsbDevice>, usbDevice: UsbDevice?) {
        usbDevices.clear()
        usbDevices.addAll(deviceList)
        mAdapter.notifyNewData(usbDevices)

        mUsbDevice = usbDevice

        if (usbDevices.isEmpty()) {
            listView.visibility = View.GONE
            tvEmptyTip.visibility = View.VISIBLE
        } else {
            listView.visibility = View.VISIBLE
            tvEmptyTip.visibility = View.GONE
        }
    }

    fun setOnDeviceItemSelectListener(listener: OnDeviceItemSelectListener?) {
        mOnDeviceItemSelectListener = listener
    }

    interface OnDeviceItemSelectListener {
        fun onItemSelect(usbDevice: UsbDevice?)
    }


}
