package org.poseestimation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.poseestimation.socketconnect.Device
import org.poseestimation.socketconnect.communication.host.FrameDataReceiver
import org.poseestimation.socketconnect.communication.slave.CommandReceiver
import org.poseestimation.videodecoder.GlobalStaticVariable
import org.poseestimation.videodecoder.GlobalStaticVariable.Companion.isFirstCreate
import kotlin.concurrent.thread

class screenReceiverActivity  : AppCompatActivity() {
    private var FrameReceiverConnectThread:Thread?=null
    lateinit var screenSurfaceView: SurfaceView
    public var mainScreenSender: Device?=null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_projection_receiver)
        hideSystemUI()
        screenSurfaceView=findViewById(R.id.screen)
        GlobalStaticVariable.isScreenCapture=true

        var bundle=intent.getExtras()
        mainScreenSender =Device(bundle!!.getString("mainScreenSenderIp"))

        screenSurfaceView.holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }
            override fun surfaceCreated(p0: SurfaceHolder) {
                FrameReceiverConnectThread=thread {
                    try {
                        FrameDataReceiver.open(p0.surface, null)
                    } catch (e: InterruptedException) {
                        Log.d("old:", "Interrupted")
                    }
                    catch (e:Throwable )
                    {
                        e.printStackTrace();
                    }
                }

            }
            override fun surfaceDestroyed(p0: SurfaceHolder) {
            }
        })
        //开始接受通信命令
        CommandReceiver.start(object : CommandReceiver.CommandListener{
            override fun onReceive(command: String?) {
                commandResolver(command)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        hideSystemUI()
    }
    private fun commandResolver(demand:String?)
    {
        when(demand) {
            "finishAcceptFrame" -> {
                CommandReceiver.close()
                finish()
            }
            null -> {
            }
        }

    }
    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onStop() {
        super.onStop()
        FrameReceiverConnectThread?.interrupt()
        FrameDataReceiver.close()
    }

}