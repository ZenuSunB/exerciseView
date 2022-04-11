package org.poseestimation.videodecoder

import android.view.Surface
import org.poseestimation.camera.CameraReceiver
import org.poseestimation.camera.CameraSender

class GlobalStaticVariable {
    companion object{
        public var frameLength:Int=640
        public var frameWidth:Int=480
        public var frameRate:Int=25
        public var isScreenCapture:Boolean=false
        public var receiverSurface:Surface?=null
    }
}