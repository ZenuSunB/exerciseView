package org.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.poseestimation.camera.CameraSource
import org.poseestimation.data.*
import org.poseestimation.ml.ModelType
import org.poseestimation.ml.MoveNet
import kotlin.random.Random
import org.poseestimation.*
class MainActivity :AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    private var device = Device.GPU
    private lateinit var msquareProgress:SquareProgress
    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController
    private lateinit var keep1:KeepCountdownView
    private lateinit var textView: TextView
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
        println("++++++++++++++++++onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        msquareProgress = findViewById(R.id.sp);
        surfaceView = findViewById(R.id.surfaceView)

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
        initView()
        msquareProgress.setCurProgress(0);
        keep1=findViewById(R.id.keep1)
        keep1.setCountdownListener(object: KeepCountdownView.CountdownListener {
            override fun onStart(){
            }
            override fun onEnd() {
            }
        })
        keep1.startCountDown()
        openCamera()
        createPoseEstimator()
    }
    override fun onStart() {
        println("++++++++++++++++++onStart")
        super.onStart()
    }
    private fun initView(){
        videoView = findViewById<VideoView>(R.id.videoView)
        val mainActivity=this
        val JsonMeg="{\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "           \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1,\n" +
                "            \"sv_path\": \"statics/video/全身/正踢腿.mp4\",\n" +
                "            \"groups\": \"2\"\n" +
                "        }]\n" +
                "}"
        videoviewrepetend= VideoViewRepetend(JsonMeg,this,videoView,this.baseContext,object:VideoViewRepetend.VideoViewRepetendListener{
            override fun onExerciseEnd(index:Int,samplevideoName:String,samplevideoTendency:MutableList<Int>) {
                //一轮运动完成，开始创建下一轮运动的数据结构
                //休息阶段时关闭图像处理
                cameraSource!!.setProcessImageFlag(false)
                //创建新一轮运动数据结构
                cameraSource!!.Samples.add(Sample("sample3-10fps.processed.json",baseContext,1,samplevideoTendency,object:Sample.scorelistener{
                    override fun onFrameScoreHeight(FrameScore: Int,part:Int) {
                        voice.voicePraise(FrameScore,part)
                    }
                    override fun onFrameScoreLow(FrameScore: Int,part:Int) {
                        voice.voiceRemind(FrameScore,part)
                    }
                }))
                cameraSource!!.Users.add(ResJSdata())

                //更新came索引，使其图像处理绑定到下一轮运动的数据结构中
                cameraSource!!.index=++(videoviewrepetend!!.index)
            }
            override fun onExerciseStart(index:Int,samplevideoName:String) {
                cameraSource!!.setProcessImageFlag(true)
            }
        })
    }
    override fun onResume() {
        println("++++++++++++++++++onResume")
        cameraSource?.resume()
        videoviewrepetend?.videoView?.start()
        super.onResume()
    }

    override fun onPause() {
        println("++++++++++++++++++onPause")
        cameraSource?.pause()
        videoviewrepetend?.videoView?.pause()
        super.onPause()
    }

    override fun onStop() {
        println("++++++++++++++++++onStop")
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
                    },this.baseContext,
                        this,
                        videoviewrepetend?.schedule!!.getTag(videoviewrepetend!!.index),
                        videoviewrepetend?.schedule!!.exerciseName.get(videoviewrepetend!!.index)).apply {
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

    override fun onKeyDown(keyCode:Int, event: KeyEvent?):Boolean {
        // TODO Auto-generated method stub
        if(keyCode==KeyEvent.KEYCODE_BACK){
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
