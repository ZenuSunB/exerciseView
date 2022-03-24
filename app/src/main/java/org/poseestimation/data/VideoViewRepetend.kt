package org.poseestimation.data

import android.app.Activity
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import org.poseestimation.MainActivity
import org.poseestimation.MesgSpeak
import org.poseestimation.R
import org.poseestimation.camera.CameraSource

class VideoViewRepetend(
    private var JSONmeg:String,
    private var mainActivity: Activity,
    public var videoView: VideoView,
    private var context: Context,
    private var listener: VideoViewRepetendListener?=null)
{

    public lateinit var schedule: ExerciseSchedule
    //记录下当前播放到那哪一组运动视频
    public var index=0
    lateinit var voicePlayer:MediaPlayer
    init {
        schedule=ExerciseSchedule(JSONmeg)
        setPlayVideo()
        }
    fun setPlayVideo()//0倒计时，1运动，2休息
    {
        videoView.setOnPreparedListener(object : MediaPlayer.OnPreparedListener{
            override fun onPrepared(mp: MediaPlayer) {
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        })

        videoView.setOnCompletionListener{
            //倒计时完毕后开始播放运动视频
            listener?.onExerciseStart(index,"11111")//运动开始触发,进入运动视频
            // uri = "android.resource://" + context.packageName + "/" +  schedule.exerciseName.get(i++)，
            val exVideoId=context.resources.getIdentifier("sample3", "raw", context.getPackageName())
            val ExerciseDounturi = "android.resource://" + context.packageName + "/" + exVideoId
            videoView.setVideoURI(Uri.parse(ExerciseDounturi));
            videoView.setOnPreparedListener { it.isLooping = false }
            videoView.start()

            videoView.setOnCompletionListener {
                //运动视频结束，开始进入休息界面
                listener?.onExerciseEnd(index,"11111",schedule.getTag(index))//运动结束触发，进入休息视频
                val reVideoId=context.resources.getIdentifier("relaxtimer", "raw", context.getPackageName())
                val Relaxingturi = "android.resource://" + context.packageName + "/" + reVideoId
                videoView.setVideoURI(Uri.parse(Relaxingturi));
                videoView.setOnPreparedListener { it.isLooping = false }
                videoView.start()
                videoView.setOnCompletionListener {
                    //休息视频运动完毕,重新开始播放倒计时
                    setPlayVideo()
                }
            }
        }

        // 进入倒计时
        val countDounturi = "android.resource://" + context.packageName + "/" + R.raw.countdown
        videoView.setVideoURI(Uri.parse(countDounturi));
        videoView.setOnPreparedListener { it.isLooping = false }
        videoView.start()

        //播放倒计时提示声音
        val fd = context.assets.openFd("voice/countdown/5countdown.mp3");
        voicePlayer=MediaPlayer()
        voicePlayer.setDataSource(fd)
        voicePlayer.prepare()
        voicePlayer.start()
        voicePlayer.setOnCompletionListener {
            it.release()
        }
    }

    interface VideoViewRepetendListener {
        fun onExerciseStart(index:Int,samplevideoName:String)
        fun onExerciseEnd(index:Int,samplevideoName:String,samplevideoTendency:MutableList<Int>)
    }
}