package org.poseestimation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity

import org.poseestimation.socketconnect.Device
import org.poseestimation.socketconnect.communication.host.FrameDataReceiver
import org.poseestimation.videodecoder.GlobalStaticVariable
import kotlin.concurrent.thread

class screenReceiverActivity  : AppCompatActivity() {
    lateinit var screenSurfaceView: SurfaceView
    public var mainScreenSender: Device?=null
    private lateinit var imageBitmap: Bitmap
    private lateinit var yuvConverter: YuvToRgbConverter
    private var FrameReceiverConnectThread:Thread?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_projection_receiver)
        FrameReceiverConnectThread?.let{
            it.interrupt()
        }
        FrameReceiverConnectThread=
            thread{
                try {
                    FrameDataReceiver.open(object : FrameDataReceiver.FrameDataListener {
                        override fun onReceive(image: Image) {
                            if (image != null) {
                                if (!::imageBitmap.isInitialized) {
                                    imageBitmap =
                                        Bitmap.createBitmap(
                                            GlobalStaticVariable.frameWidth,
                                            GlobalStaticVariable.frameLength,
                                            Bitmap.Config.ARGB_8888
                                        )
                                }
                                yuvConverter.yuvToRgb(image, imageBitmap)
                                val holder = screenSurfaceView.holder
                                val surfaceCanvas = holder.lockCanvas()
                                surfaceCanvas?.let { canvas ->
                                    val screenWidth: Int
                                    val screenHeight: Int
                                    val left: Int
                                    val top: Int

                                    if (canvas.height > canvas.width) {
                                        val ratio = imageBitmap.height.toFloat() / imageBitmap.width
                                        screenWidth = canvas.width
                                        left = 0
                                        screenHeight = (canvas.width * ratio).toInt()
                                        top = (canvas.height - screenHeight) / 2
                                    } else {
                                        val ratio = imageBitmap.width.toFloat() / imageBitmap.height
                                        screenHeight = canvas.height
                                        top = 0
                                        screenWidth = (canvas.height * ratio).toInt()
                                        left = (canvas.width - screenWidth) / 2
                                    }
                                    val right: Int = left + screenWidth
                                    val bottom: Int = top + screenHeight

                                    canvas.drawBitmap(
                                        imageBitmap, Rect(0, 0, imageBitmap.width, imageBitmap.height),
                                        Rect(left, top, right, bottom), null
                                    )
                                    screenSurfaceView.holder.unlockCanvasAndPost(canvas)
                                }
                                image.close()
                            }
                        }
                    })
                }
                catch (e:InterruptedException)
                {
                    Log.d("old:","Interrupted")
                }
            }
        hideSystemUI()

        screenSurfaceView=findViewById(R.id.screen)
        yuvConverter=YuvToRgbConverter(screenSurfaceView.context)
        var bundle=intent.getExtras()
        mainScreenSender =Device(bundle!!.getString("mainScreenSenderIp"))


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
//        FrameDataReceiver.close()
    }

}