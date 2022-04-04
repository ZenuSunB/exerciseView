package org.poseestimation.socketconnect.communication.host;

import android.media.Image;
import android.util.Log;

import androidx.annotation.Nullable;

import org.poseestimation.camera.CameraReceiver;
import org.poseestimation.socketconnect.RemoteConst;
import org.poseestimation.socketconnect.communication.CommunicationKey;
import org.poseestimation.videodecoder.DecoderH264;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FrameDataReceiver {

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60,
            TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ReceiveFrameDataThreadFactory(), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            throw new RejectedExecutionException();
        }
    });
    private static FrameDataReceiver.FrameDataListener listener;
    private static DecoderH264 decoder;
    private static volatile boolean isOpen;


    public static void open(FrameDataReceiver.FrameDataListener frameDataListener){
        listener = frameDataListener;
        isOpen = true;
        decoder=new DecoderH264(CameraReceiver.PREVIEW_WIDTH, CameraReceiver.PREVIEW_HEIGHT,
                new DecoderH264.DecoderListener() {
            @Override
            public void YUV420(@Nullable Image image) {
                if(listener!=null)
                {
                    listener.onReceive(image);
                }
            }
        });
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(RemoteConst.FRAME_RECEIVE_PORT);
                    while(isOpen){
                        Socket socket = serverSocket.accept();
                        threadPool.execute(new FrameDataReceiver.FrameDataParseRunnable(socket));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void close(){
        isOpen = false;
    }

    public static class FrameDataParseRunnable implements Runnable{
        Socket socket;
        public FrameDataParseRunnable(Socket socket) {this.socket = socket;}
        byte[] bytes = new byte[1024*256];
        @Override
        public void run() {
            try {
                DataInputStream is = new DataInputStream(socket.getInputStream());
                int FrameLength= 0;
                int type=0;
                //0还没开始读，1读到了<,2读到了<<,3读到了<<<,4读到了<<<<,
                //5读到了<<<<<,6读到了<<<<<<,7读到了<<<<<<<,8读到了<<<<<<<<
                //9读到了<<<<<<<<****>,
                int FrameLengthOffSet=0;
                while(true)
                {
                    byte b = (byte) is.read();
                    if (b == -1) {
                        break;
                    }
                    if((char)b==CommunicationKey.FRAMEHEAD_BEGIN)
                    {
                        assert type<=8;
                        type+=1;
                        if(type==8)
                        {
                            while(FrameLengthOffSet<4)
                            {
                                b = (byte) is.read();
                                FrameLength |=
                                        (b << FrameLengthOffSet * 8) & (0xFF << FrameLengthOffSet * 8);
                                FrameLengthOffSet++;
                            }
                            assert FrameLengthOffSet==4;
                            int readFrameLength=0;
                            int remainFrameLength=FrameLength;
                            while(remainFrameLength>0) {
                                int newReaded=is.read(bytes, readFrameLength, remainFrameLength);
                                readFrameLength+=newReaded;
                                remainFrameLength-=newReaded;
                            }
                            if(decoder!=null){
                                Log.d("TTTTTT", "readData: "+FrameLength);
                                byte[] resData=new byte[FrameLength];
                                System.arraycopy(bytes,0,resData,0,FrameLength);
                                decoder.decoderH264(resData);
                            }
                            FrameLength=0;
                            FrameLengthOffSet=0;
                            type=0;
                        }
                        continue;
                    }
                    else
                    {
                        type=0;
                        continue;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public interface FrameDataListener{
        void onReceive(Image image);
    }
}
