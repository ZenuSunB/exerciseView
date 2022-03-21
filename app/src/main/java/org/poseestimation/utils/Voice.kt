package org.poseestimation.utils

import android.content.Context
import android.media.MediaPlayer
import kotlin.random.Random

class Voice(private val context: Context) {
    var voicePlayer= MediaPlayer()
    public fun voicePraise(FrameScore: Int,part:Int)
    {

    }
    public fun voiceRemind(FrameScore: Int,part:Int)
    {
        var voicePath="voice/"
        var random :Int = 0
        when(part)
        {
            0-> {
                voicePath += "head/"//头部
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            1-> {
                voicePath += "left_arm/"//左手臂
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            2-> {
                voicePath += "right_arm/"//右手臂
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            3-> {
                voicePath += "left_leg/"//左腿
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            4-> {
                voicePath += "right_leg/"//右腿
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            5-> {
                voicePath += "waist/"//腰部
                random = Random(7).nextInt(2)//0 1 2随机数
            }
            6-> {
                voicePath += "shoulder/"//肩膀
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            7->{
                voicePath+="neck/"//颈部
                random = Random(7).nextInt(3)//0 1 2随机数
            }
            8->{
                voicePath+="torso/"//躯干
                random = 0
            }
        }
        voicePath+=random.toString()+".mp3"
        voicePlayer= MediaPlayer()
        val fd = context.assets.openFd(voicePath);
        voicePlayer.setDataSource(fd)
        voicePlayer.prepare()
        voicePlayer.start()
    }
}