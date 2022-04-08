package org.poseestimation.socketconnect.connectview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.poseestimation.R
import org.poseestimation.ReceiverActivity
import org.poseestimation.layoutImpliment.BackArrowView
import org.poseestimation.layoutImpliment.connectAdapter
import org.poseestimation.socketconnect.Device
import org.poseestimation.socketconnect.communication.host.Command
import org.poseestimation.socketconnect.communication.host.CommandSender
import org.poseestimation.socketconnect.search.DeviceSearcher


class hostviewActivity: AppCompatActivity() {
    private var mContext: Context?=null
    lateinit var view: View
    lateinit var btnSearchDeviceOpen : Button
    lateinit var slaveList: ListView
    lateinit var btnReturn:BackArrowView
    var isSearchDeviceOpen:Boolean=false;
    var devices: MutableMap<String, Device> = mutableMapOf()
    fun sendCommand(device: Device) {
        //发送命令
        val command = Command("openCamera".toByteArray(), object : Command.Callback {
            override fun onEcho(msg: String?) {
            }
            override fun onError(msg: String?) {
            }
            override fun onRequest(msg: String?) {
            }
            override fun onSuccess(msg: String?) {
            }
        })
        command.setDestIp(device.ip)
        CommandSender.addCommand(command)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.remote_camera_launcher)
        btnSearchDeviceOpen=this.findViewById(R.id.connectBtn)
        btnReturn=this.findViewById(R.id.back_arrow)
        btnReturn.setOnClickListener{
            System.exit(0)
        }
        slaveList=this.findViewById(R.id.slavelist)
//        slaveList.setOnItemClickListener(object : AdapterView.OnItemClickListener {
//            override fun onItemClick(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                val listView = p0 as ListView
//                val listAdapter = listView.adapter
//                val choosed: String = listAdapter.getItem(position)  as String
//                devices.get(choosed)?.let {
//                    sendCommand(it)
//                }
//                val intent = Intent(baseContext, ReceiverActivity::class.java)
//                var slaveIp=devices.get(choosed)!!.ip
//                intent.putExtra("slaveIP",slaveIp)
//                clear()
//                stopSearch()
//                isSearchDeviceOpen = false
//                btnSearchDeviceOpen.setText("开始搜索")
//                startActivity(intent)
//            }
//        })
        btnSearchDeviceOpen.setOnClickListener{
            //创建popview进行局域网搜索
                if (isSearchDeviceOpen) {
                    //设备搜索已关闭
                    clear()
                    stopSearch()
                    isSearchDeviceOpen = false
                    btnSearchDeviceOpen.setText("开始搜索")
                    Toast.makeText(this, "设备搜索已关闭", Toast.LENGTH_SHORT).show()

                } else {
                    //设备搜索开始
                    isSearchDeviceOpen = true
                    btnSearchDeviceOpen.setText("搜索关闭")
                    Toast.makeText(this, "设备搜索开始", Toast.LENGTH_SHORT).show()
                    startSearch()
                }

        }


    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
    /**
     * 开始异步搜索局域网中的设备
     */
    private fun startSearch() {
        DeviceSearcher.search(object : DeviceSearcher.OnSearchListener{
            override fun onSearchFinish() {
//                Toast.makeText(mContext,"",Toast.LENGTH_SHORT).show()
            }
            override fun onSearchStart() {
//                Toast.makeText(mContext,"新设备发现",Toast.LENGTH_SHORT).show()
            }
            override fun onSearchedNewOne(device: Device?) {
                device?.let {
                    devices.put(it.uuid,it)
                    var slaveList_str: ArrayList<String> = arrayListOf()
                    for(item in devices)
                    {
                        slaveList_str.add(item.value.uuid)
                    }
                    var adapter = connectAdapter(slaveList_str,object :View.OnClickListener{
                        override fun onClick(view: View) {
                            var uuid = view.getTag() as String
                            devices.get(uuid)?.let {
                                sendCommand(it)
                            }
                            val intent = Intent(baseContext, ReceiverActivity::class.java)
                            var slaveIp=devices.get(uuid)!!.ip
                            intent.putExtra("slaveIp",slaveIp)
                            clear()
                            stopSearch()
                            isSearchDeviceOpen = false
                            btnSearchDeviceOpen.setText("开始搜索")
                            startActivity(intent)
                        }
                    })
                    slaveList.adapter=adapter
                }
            }
        })
    }
    private fun stopSearch() {
        DeviceSearcher.close()
        slaveList.setAdapter(
            ArrayAdapter<String>(
            baseContext,
            android.R.layout.simple_list_item_1,
            arrayListOf())
        )
       devices.clear()
    }
    private fun clear()
    {
        slaveList.setAdapter(
            ArrayAdapter<String>(
            baseContext,
            android.R.layout.simple_list_item_1,
            arrayListOf())
        )
        devices.clear()
    }
}