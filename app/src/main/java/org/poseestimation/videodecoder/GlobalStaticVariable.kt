package org.poseestimation.videodecoder

import org.poseestimation.camera.CameraReceiver
import org.poseestimation.camera.CameraSender

class GlobalStaticVariable {
    companion object{
        public var frameLength:Int=480
        public var frameWidth:Int=640
        public var frameRate:Int=25
    }
}