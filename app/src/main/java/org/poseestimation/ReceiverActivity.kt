package org.poseestimation

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import org.poseestimation.socketconnect.Device
import org.poseestimation.socketconnect.communication.CommunicationKey
import org.poseestimation.socketconnect.communication.host.FrameDataReceiver
import org.poseestimation.socketconnect.communication.slave.CommandReceiver
import org.poseestimation.socketconnect.connectpopview.hostPopView
import org.poseestimation.socketconnect.connectpopview.slavePopView
import org.poseestimation.socketconnect.search.DeviceSearchResponser

class ReceiverActivity: AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }
    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    private var isSearchDeviceOpen:Boolean=false
    private val device: Device? =null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        findViewById<CoordinatorLayout>(R.id.main_layout).post {
            //创建popview进行局域网搜索
            val hostpopView = hostPopView()

            //开始接受frame
            FrameDataReceiver.open(object : FrameDataReceiver.FrameDataListener{
                override fun onReceive(data: String?) {

                }
            })
            hostpopView.CreateRegisterPopWindow(this, View.OnClickListener {
                if (isSearchDeviceOpen) {
                    //设备搜索已关闭
                    hostpopView.clear()
                    hostpopView.stopSearch()
                    isSearchDeviceOpen = false
                    hostpopView.btnSearchDeviceOpen.setText("开始搜索局域网下的设备")
                    Toast.makeText(this, "设备搜索已关闭", Toast.LENGTH_SHORT).show()
                } else {
                    //设备搜索开始
                    isSearchDeviceOpen = true
                    hostpopView.btnSearchDeviceOpen.setText("设备搜索关闭")
                    Toast.makeText(this, "设备搜索开始", Toast.LENGTH_SHORT).show()
                    hostpopView.startSearch()

                }
            })
            hostpopView.showAtLocation(
                this.findViewById(R.id.main_layout),
                Gravity.CENTER,
                0,
                0
            )
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }





    override fun onKeyDown(keyCode:Int, event: KeyEvent?):Boolean {
        // TODO Auto-generated method stub
        if(keyCode== KeyEvent.KEYCODE_BACK){
            val msg="您的本次运动记录将不会保存，确定退出码？"
            AlertDialog.Builder(this)
                .setMessage(msg)
                .setTitle("注意")
                .setPositiveButton("确认", DialogInterface.OnClickListener { dialogInterface, i ->
                    finish()
                })
                .setNeutralButton("取消", null)
                .create()
                .show()
            return false
        }
        else {
            return false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()


        companion object {
            @JvmStatic
            private val ARG_MESSAGE = "message"
            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}