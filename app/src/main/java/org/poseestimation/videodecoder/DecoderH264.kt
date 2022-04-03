package org.poseestimation.videodecoder

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.nio.ByteBuffer


class DecoderH264(
    private val width:Int,
    private val height:Int,
    private var listener: DecoderListener? = null) {
    private var frameRate: Int = 30
    private val COLOR_FormatI420 = 1
    private val COLOR_FormatNV21 = 2
    private fun isImageFormatSupported(image: Image): Boolean {
        val format = image.format
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
        }
        return false
    }

    private lateinit var mediaCodec: MediaCodec
    init {
        mediaCodec = MediaCodec.createDecoderByType("video/avc")
        //height和width一般都是照相机的height和width。
        var mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
        //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
        //描述视频格式的帧速率（以帧/秒为单位）的键。帧率，一般在15至30之内，太小容易造成视频卡顿。
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        //关键帧间隔时间，单位是秒
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)

        mediaCodec.configure(mediaFormat,null, null, 0)
        //开始编码
        mediaCodec.start()
    }
    public fun decoderH264(byteArray: ByteArray){
        //拿到输入缓冲区,用于传送数据进行解码
        var inputBuffers = mediaCodec.inputBuffers
        //拿到输出缓冲区,用于取到解码后的数据
        val outputBuffers = mediaCodec.outputBuffers
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
        //当输入缓冲区有效时,就是>=0
        if (inputBufferIndex >= 0) {
            var inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            //往输入缓冲区写入数据
            inputBuffer.put(byteArray)
            //五个参数，第一个是输入缓冲区的索引，第二个数据是输入缓冲区起始索引，第三个是放入的数据大小，第四个是时间戳，保证递增就是
            mediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                byteArray.count (),
            System.nanoTime() / 1000,
            0
            )
        }
        val bufferInfo = MediaCodec.BufferInfo()
        //拿到输出缓冲区的索引

        var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        while (outputBufferIndex >= 0) {
            var outputBuffer = outputBuffers[outputBufferIndex]
            var outData = ByteArray(bufferInfo.size)
            outputBuffer.get(outData);

            listener?.YUV420(mediaCodec.getOutputImage(outputBufferIndex))

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private fun getDataFromImage(image: Image, colorFormat: Int): ByteArray? {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
        if (!isImageFormatSupported(image)) {
            throw RuntimeException("can't convert Image to byte array, format " + image.format)
        }
        val crop: Rect = image.cropRect
        val format = image.format
        val width: Int = crop.width()
        val height: Int = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer: ByteBuffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }

    interface DecoderListener
    {
        fun YUV420(image: Image?)
    }
}