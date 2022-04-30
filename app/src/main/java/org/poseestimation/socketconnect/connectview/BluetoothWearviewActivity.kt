package org.poseestimation.socketconnect.connectview

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.poseestimation.R
import org.poseestimation.layoutImpliment.BackArrowView
import org.poseestimation.layoutImpliment.connectAdapter
import org.poseestimation.socketconnect.bluetoothReceiver.ScanBroadcastReceiver
import java.lang.reflect.Method


class BluetoothWearviewActivity : AppCompatActivity() {
    lateinit var btnSearchDeviceOpen : Button
    lateinit var receiverList: ListView
    lateinit var btnReturn: BackArrowView
    var isSearchDeviceOpen:Boolean=false;
    var devices: MutableMap<String, BluetoothDevice> = mutableMapOf()
    var choosed_device: BluetoothDevice?=null

    private var mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var scanBroadcastReceiver: ScanBroadcastReceiver?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear)
        btnReturn=findViewById(R.id.back_arrow)
        btnSearchDeviceOpen=this.findViewById(R.id.connectBtn)
        btnReturn=this.findViewById(R.id.back_arrow)
        btnReturn.setOnClickListener{
            finish()
        }
        try {
            if(mAdapter==null||!mAdapter.isEnabled())
            {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
            val pairedDevices: Set<BluetoothDevice> = mAdapter.getBondedDevices()
            for(i in pairedDevices)
            {
                println("++++++++++++++"+i.name)
            }
            receiverList=this.findViewById(R.id.slavelist)
            btnSearchDeviceOpen.setOnClickListener{
                //创建popview进行局域网搜索
                if (isSearchDeviceOpen) {
                    clear()
                    mAdapter.cancelDiscovery()
                    isSearchDeviceOpen = false
                    btnSearchDeviceOpen.setText("开始搜索")
                    Toast.makeText(this, "设备搜索已关闭", Toast.LENGTH_SHORT).show()

                } else {
                    mAdapter.startDiscovery();
                    isSearchDeviceOpen = true
                    btnSearchDeviceOpen.setText("搜索关闭")
                    Toast.makeText(this, "设备搜索开始", Toast.LENGTH_SHORT).show()
                }

            }
        }catch (e:SecurityException)
        {
            e.printStackTrace()
        }

    }
    fun clear(){
        receiverList.setAdapter(
            ArrayAdapter<String>(
                baseContext,
                android.R.layout.simple_list_item_1,
                arrayListOf())
        )
        devices.clear()
    }
    override fun onStart() {
        super.onStart()

    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(scanBroadcastReceiver);
    }

    override fun onResume() {
        super.onResume()
        registerScanBroadcast()
    }

    override fun onPause() {
        super.onPause()
    }
    /**
     * 使用新api扫描
     * 注册蓝牙扫描监听
     */
    fun registerScanBroadcast() {
        val application: Application = this.application
        //注册蓝牙扫描状态广播接收者
        if (scanBroadcastReceiver == null && application != null) {
            scanBroadcastReceiver=ScanBroadcastReceiver(object :ScanBroadcastReceiver.ScanBroadcastReceiverListener{
                override fun on_found(device: BluetoothDevice) {
                    device?.let {
                        try {
                            devices.put(it.name, it)
                            var slaveList_str: ArrayList<String> = arrayListOf()
                            for (item in devices) {
                                slaveList_str.add(item.value.name)
                            }
                            var adapter =
                                connectAdapter(slaveList_str, object : View.OnClickListener {
                                    override fun onClick(view: View) {
                                        var name = view.getTag() as String
                                        devices.get(name)?.let {
                                            choosed_device = it
                                            if (mAdapter.isDiscovering()) {
                                                mAdapter.cancelDiscovery();
                                            }
                                            it.createBond()
//                                            val createBond: Method =
//                                                BluetoothDevice::class.java.getMethod("createBond")
//                                            createBond.invoke(it)
                                        }
                                    }
                                })
                            receiverList.adapter = adapter
                        }catch (e:SecurityException)
                        {
                            e.printStackTrace()
                        }
                    }
                }
            })
            val filter = IntentFilter()
            //开始扫描
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            //扫描结束
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            //扫描中，返回结果
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            //扫描模式改变
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            //注册广播接收监听，用完不要忘了解注册哦
            application.registerReceiver(scanBroadcastReceiver, filter)
        }
    }



}