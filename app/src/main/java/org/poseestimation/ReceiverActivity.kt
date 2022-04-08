package org.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.poseestimation.camera.CameraReceiver
import org.poseestimation.data.*
import org.poseestimation.layoutImpliment.SquareProgress
import org.poseestimation.ml.ModelType
import org.poseestimation.ml.MoveNet
import org.poseestimation.socketconnect.Device
import org.poseestimation.socketconnect.communication.host.Command
import org.poseestimation.socketconnect.communication.host.CommandSender
import org.poseestimation.socketconnect.communication.host.FrameDataReceiver
import org.poseestimation.socketconnect.connectpopview.hostPopView
import kotlin.concurrent.thread
import kotlin.math.log

class ReceiverActivity: AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
        public var mainSlave:Device?=null
    }
    /** A [SurfaceView] for remote camera preview.   */
    private lateinit var surfaceView: SurfaceView
//    private lateinit var hostpopView : hostPopView
//    private var isSearchDeviceOpen:Boolean=false


    private var device = org.poseestimation.data.Device.GPU
    private lateinit var msquareProgress: SquareProgress
    private lateinit var videoView: VideoView
    private lateinit var countdownView: SurfaceView
    private lateinit var countdownViewFramLayout: FrameLayout
    private var cameraReceiver: CameraReceiver? = null
    private val voice= org.poseestimation.utils.Voice(this)
    private var videoviewrepetend: VideoViewRepetend? =null
    private var FrameReceiverConnectThread:Thread?=null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                MainActivity.ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, ReceiverActivity.FRAGMENT_DIALOG)
            }
        }
    fun sendCommand(device: Device) {
        //发送命令
        val command = Command("sendFrame".toByteArray(), object : Command.Callback {
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
        setContentView(R.layout.activity_receiver)
        //accept intent value
        var bundle=intent.getExtras()
        mainSlave=Device(bundle!!.getString("slaveIp"))
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        msquareProgress = findViewById(R.id.sp);
        countdownView= findViewById(R.id.countDownView)
        surfaceView = findViewById(R.id.surfaceView)
        videoView = findViewById(R.id.videoView)
        countdownViewFramLayout=findViewById(R.id.countDownViewLayout)

//        findViewById<CoordinatorLayout>(R.id.main_layout).post {
//            //创建popview进行局域网搜索
//            hostpopView = hostPopView()
//            hostpopView.CreateRegisterPopWindow(this, View.OnClickListener {
//                if (isSearchDeviceOpen) {
//                    //设备搜索已关闭
//                    hostpopView.clear()
//                    hostpopView.stopSearch()
//                    isSearchDeviceOpen = false
//                    hostpopView.btnSearchDeviceOpen.setText("开始搜索局域网下的设备")
//                    Toast.makeText(this, "设备搜索已关闭", Toast.LENGTH_SHORT).show()
//
//                } else {
//                    //设备搜索开始
//                    isSearchDeviceOpen = true
//                    hostpopView.btnSearchDeviceOpen.setText("设备搜索关闭")
//                    Toast.makeText(this, "设备搜索开始", Toast.LENGTH_SHORT).show()
//                    hostpopView.startSearch()
//                }
//            })
//            hostpopView.showAtLocation(
//                this.findViewById(R.id.main_layout),
//                Gravity.CENTER,
//                0,
//                0
//            )
//        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()) {
            Toast.makeText(this, "已获得访问所有文件的权限", Toast.LENGTH_SHORT).show();
        } else {
            var intent: Intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }


        initView()
        msquareProgress.setCurProgress(0);
        openCamera()
        createPoseEstimator()

    }
    private fun initView(){
        val mainActivity=this
        val JsonMeg="{\n" +
                "    \"id\": 1,\n"+
                "    \"data\": [\n" +
                "        {\n" +
                "            \"id\": 5,\n" +
                "            \"url\": \"sample5\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 7,\n" +
                "            \"url\": \"sample7\",\n" +
                "            \"groups\": \"2\"\n" +
                "        }]}"

        videoviewrepetend= VideoViewRepetend(JsonMeg,this,videoView,countdownView,countdownViewFramLayout,this.baseContext,object:VideoViewRepetend.VideoViewRepetendListener{
            override fun onExerciseEnd(index:Int,samplevideoName:String,samplevideoTendency:MutableList<Int>) {
                //一轮运动完成，开始创建下一轮运动的数据结构
                //休息阶段时关闭图像处理
                cameraReceiver!!.setProcessImageFlag(false)
                //创建新一轮运动数据结构
                cameraReceiver!!.Samples.add(Sample(samplevideoName+".processed.json",baseContext,index,samplevideoTendency,object:
                    Sample.scorelistener{
                    override fun onFrameScoreHeight(FrameScore: Int,part:Int) {
                        voice.voicePraise(FrameScore,part)
                    }
                    override fun onFrameScoreLow(FrameScore: Int,part:Int) {
                        voice.voiceRemind(FrameScore,part)
                    }
                }))

                thread {
                    cameraReceiver!!.Users.get(index-1).writeTofile("test", cameraReceiver!!.Samples[index-1].getSampleVecList(),baseContext)
                }

                //创建新的用户数据收集器
                cameraReceiver!!.Users.add(ResJSdata(index))

                //更新came索引，使其图像处理绑定到下一轮运动的数据结构中
                cameraReceiver!!.index++
            }
            override fun onExerciseStart(index:Int,samplevideoName:String) {
                cameraReceiver!!.setProcessImageFlag(true)
            }

            override fun onExerciseFinish(index: Int) {
                //运动全部结束，准备退出
                //退出前关闭图像处理
                cameraReceiver!!.setProcessImageFlag(false)
                thread {
                    cameraReceiver!!.Users.get(index - 1).writeTofile(
                        "test",
                        cameraReceiver!!.Samples[index - 1].getSampleVecList(),
                        baseContext
                    )
                }
                cameraReceiver!!.index++
            }
        })
    }

    override fun onResume() {
        cameraReceiver?.resume()
//        videoviewrepetend?.videoView?.start()
        super.onResume()
    }

    override fun onPause() {
        cameraReceiver?.pause()
//        videoviewrepetend?.videoView?.pause()
//        hostpopView.dismiss()
        cameraReceiver?.close()
        FrameDataReceiver.close()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
//        hostpopView.dismiss()
        cameraReceiver?.close()
        FrameDataReceiver.close()
        FrameReceiverConnectThread?.let{
            it.interrupt()
        }
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return true
    }


    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraReceiver == null) {
                cameraReceiver =
                    CameraReceiver(surfaceView, object : CameraReceiver.CameraReceiverListener {
                        override fun onImageprocessListener(score: Int) {
                            msquareProgress.setCurProgress(score);
                        }
                        override fun onDetectedInfo( personScore: Float?,poseLabels: List<Pair<String, Float>>?) {
                            TODO("Not yet implemented")
                        }
                        override fun onFPSListener(fps: Int) {
                            showToast("fps:"+fps.toString())
                        }
                        override fun onPersonDetected()
                        {
                            videoviewrepetend?.start()
                        }
                    },this.baseContext,
                        this,
                        //*************************************************************
                        ExerciseSchedule.getTag(videoviewrepetend!!.index),
                        //*************************************************************
                        ExerciseSchedule.exerciseName.get(videoviewrepetend!!.index)).apply {
                        FrameReceiverConnectThread?.let{
                            it.interrupt()
                        }
                        FrameReceiverConnectThread=
                            thread{
                                try {
                                    prepareCamera()
                                }
                                catch (e:InterruptedException)
                                {
                                    Log.d("old:","Interrupted")
                                }
                            }
                        }
                        //*************************************************************
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraReceiver?.initCamera()
                }
                sendCommand(mainSlave!!)
            }
        }
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraReceiver?.setDetector(detector)
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
        if(keyCode== KeyEvent.KEYCODE_BACK){
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