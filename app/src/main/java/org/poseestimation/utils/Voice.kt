package org.poseestimation.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import java.sql.Time
import java.util.*
import kotlin.random.Random

class Voice(private val context: Context) {
    private val lock = Any()
    private var voicePlayer= MediaPlayer()
    public fun voicePraise(FrameScore: Int,part:Int)
    {
        synchronized(lock)
        {
            var voicePath = "voice/great/"
            var random: Int = 0
            random = (Math.random() * 10).toInt() % 2//0 1 2随机数
            voicePath += random.toString() + ".mp3"
            voicePlayer?.release()
            voicePlayer = MediaPlayer()
            voicePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            val fd = context.assets.openFd(voicePath);
            voicePlayer.setDataSource(fd)
            voicePlayer.prepare()
            voicePlayer.start()

        }
    }
    public fun voiceRemind(FrameScore: Int,part:Int)
    {
        synchronized(lock)
        {
            var voicePath = "voice/"
            var random: Int = 0
            when (part) {
                0 -> {
                    voicePath += "head/"//头部
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                1 -> {
                    voicePath += "left_arm/"//左手臂
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                2 -> {
                    voicePath += "right_arm/"//右手臂
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                3 -> {
                    voicePath += "left_leg/"//左腿
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                4 -> {
                    voicePath += "right_leg/"//右腿
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                5 -> {
                    voicePath += "waist/"//腰部
                    random = (Math.random() * 10).toInt() % 2//0 1 随机数
                }
                6 -> {
                    voicePath += "shoulder/"//肩膀
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                7 -> {
                    voicePath += "neck/"//颈部
                    random = (Math.random() * 10).toInt() % 3//0 1 2随机数
                }
                8 -> {
                    voicePath += "torso/"//躯干
                    random = 0
                }
            }
            voicePath += random.toString() + ".mp3"
            voicePlayer?.release()
            voicePlayer = MediaPlayer()
            voicePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            val fd = context.assets.openFd(voicePath);
            voicePlayer.setDataSource(fd)
            voicePlayer.prepare()
            voicePlayer.start()
        }
    }
}