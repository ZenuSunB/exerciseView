package org.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.poseestimation.camera.CameraSource
import org.poseestimation.data.*
import org.poseestimation.layoutImpliment.SquareProgress
import org.poseestimation.ml.ModelType
import org.poseestimation.ml.MoveNet
import java.io.FileOutputStream
import kotlin.concurrent.thread

class MainActivity :AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }
    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    private var device = Device.GPU
    private lateinit var msquareProgress: SquareProgress
    private lateinit var videoView: VideoView
    private lateinit var countdownView: SurfaceView
    private lateinit var countdownViewFramLayout: FrameLayout
    private var cameraSource: CameraSource? = null

    private val voice= org.poseestimation.utils.Voice(this)
    private var videoviewrepetend:VideoViewRepetend? =null

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
        setContentView(R.layout.activity_main)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        msquareProgress = findViewById(R.id.sp);
        countdownView= findViewById(R.id.countDownView)
        surfaceView = findViewById(R.id.surfaceView)
        videoView = findViewById(R.id.videoView)
        countdownViewFramLayout=findViewById(R.id.countDownViewLayout)

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()) {
            Toast.makeText(this, "已获得访问所有文件的权限", Toast.LENGTH_SHORT).show();
        } else {
            var intent:Intent =Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
        initView()
        msquareProgress.setCurProgress(0);

        openCamera()
        createPoseEstimator()
    }
    override fun onStart() {
        super.onStart()
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
            override fun onExerciseEnd(index:Int,samplevideoName:String,samplevideoTendency:MutableList<Int>,id:Int) {
                //一轮运动完成，开始创建下一轮运动的数据结构
                //休息阶段时关闭图像处理
                cameraSource!!.setProcessImageFlag(false)
                //创建新一轮运动数据结构
                cameraSource!!.Samples.add(Sample(samplevideoName+".processed.json",baseContext,id,samplevideoTendency,object:Sample.scorelistener{
                    override fun onFrameScoreHeight(FrameScore: Int,part:Int) {
                        voice.voicePraise(FrameScore,part)
                    }
                    override fun onFrameScoreLow(FrameScore: Int,part:Int) {
                        voice.voiceRemind(FrameScore,part)
                    }

                    override fun onPersonNotDect() {
                        voice.voiceTips()
                    }
                }))

                thread {
                    cameraSource!!.Users.get(index - 1).exec()
                    cameraSource!!.Users.get(index - 1).toJson()
//                  cameraSource!!.Users.get(index-1).writeTofile("test", cameraSource!!.Samples[index-1].getSampleVecList(),baseContext)
                }

                //创建新的用户数据收集器
                cameraSource!!.Users.add(ResJSdata(id))

                //更新came索引，使其图像处理绑定到下一轮运动的数据结构中
                cameraSource!!.index++
            }
            override fun onExerciseStart(index:Int,samplevideoName:String) {
                cameraSource!!.setProcessImageFlag(true)
            }

            override fun onExerciseFinish(index: Int) {
                //运动全部结束，准备退出
                //退出前关闭图像处理
                cameraSource!!.setProcessImageFlag(false)
                thread {
//                    cameraSource!!.Users.get(index - 1).writeTofile(
//                        "test",
//                        cameraSource!!.Samples[index - 1].getSampleVecList(),
//                        baseContext
//                    )
                    cameraSource!!.Users.get(index - 1).exec()
                    cameraSource!!.Users.get(index - 1).toJson()
                    var TotalReturnData:JSONObject= JSONObject()
                    var TotalReturnValue:JSONArray= JSONArray()
                    for(i in 0..index-1)
                    {
                        var LineReturnValue= JSONObject()
                        LineReturnValue.put("id",cameraSource!!.Samples.get(i).getId())
                        LineReturnValue.put("data",cameraSource!!.Users.get(i).getJsonData())
                        TotalReturnValue.put(LineReturnValue)
                    }
                    TotalReturnData.put("id",ExerciseSchedule.getTotalId())
                    TotalReturnData.put("data",TotalReturnValue)
                    writeTofile("test",TotalReturnData.toString())
                }

            }
        })
    }
    override fun onResume() {
        cameraSource?.resume()
//        videoviewrepetend?.videoView?.start()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.pause()
//        videoviewrepetend?.videoView?.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
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
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
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
                        ExerciseSchedule.getTagByIndex(videoviewrepetend!!.index),
                        //*************************************************************
                        ExerciseSchedule.getName(videoviewrepetend!!.index),
                        //*************************************************************
                        ExerciseSchedule.getId(0)).apply {
                        //*************************************************************
                        prepareCamera()
                    }
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
        }
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
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
    private fun writeTofile(filename:String, Jsondata:String)
    {
        var path=filename+".txt"
        var fos: FileOutputStream = baseContext.openFileOutput(path, Context.MODE_PRIVATE)
        fos.write(Jsondata.toByteArray());
        fos.flush();
        fos.close();
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
