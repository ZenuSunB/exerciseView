package org.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.poseestimation.camera.CameraSender

import org.poseestimation.socketconnect.communication.slave.CommandReceiver
import org.poseestimation.socketconnect.communication.slave.FrameDataSender
import org.poseestimation.socketconnect.connectpopview.slavePopView
import kotlin.concurrent.thread


class SenderActivity :AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }
    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView
    private lateinit var slavepopView: slavePopView
    private var cameraSender: CameraSender? = null
    private var isResponseOpen:Boolean=false
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sender)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = findViewById(R.id.surfaceView)

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }

        findViewById<CoordinatorLayout>(R.id.main_layout).post{
            //创建popview进行局域网应答
            slavepopView=slavePopView(this)
            slavepopView.CreateRegisterPopWindow(this,View.OnClickListener {
                if(isResponseOpen)
                {
                    //停在响应搜索
                    slavepopView.stopListen()
                    isResponseOpen=false
                    slavepopView.btnResponseOpen.setText("打开应答")
                    //停止接受通信命令
                    CommandReceiver.close()
                    Toast.makeText(this, "已经关闭响应", Toast.LENGTH_SHORT).show()
                    cameraSender?.close()
                    cameraSender=null
                }
                else{
                    //开始响应搜索
                    slavepopView.startListen()
                    isResponseOpen=true

                    slavepopView.btnResponseOpen.setText("关闭应答")
                    //开始接受通信命令
                    CommandReceiver.open(object : CommandReceiver.CommandListener{
                        override fun onReceive(command: String?) {
                            commandResolver(command)
                        }
                    })
                    Toast.makeText(baseContext, "已经打开响应程序！", Toast.LENGTH_SHORT).show()
                }
            })
            slavepopView.showAtLocation(
                findViewById<CoordinatorLayout>(R.id.main_layout),
                Gravity.CENTER,
                0,
                0
            )
        }

    }

    override fun onResume() {
        cameraSender?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSender?.pause()

        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        slavepopView.dismiss()
        cameraSender?.close()
        FrameDataSender.close()
    }



    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraSender == null) {
                cameraSender =
                    CameraSender(surfaceView,baseContext)
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSender?.initCamera()
                }
            }
        }
    }

    private fun commandResolver(demand:String?)
    {
        when(demand)
        {
            "openCamera"->{
                openCamera()
            }
            "sendFrame"->{
                SystemClock.sleep(80)
                cameraSender?.let {
                    FrameDataSender.open(slavePopView.hostDevice)
                }
            }
            null->{

            }
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }


    override fun onKeyDown(keyCode:Int, event: KeyEvent?):Boolean {
        // TODO Auto-generated method stub
        if(keyCode==KeyEvent.KEYCODE_BACK){
            val msg="您的本次运动记录将不会保存，确定退出吗？"
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
