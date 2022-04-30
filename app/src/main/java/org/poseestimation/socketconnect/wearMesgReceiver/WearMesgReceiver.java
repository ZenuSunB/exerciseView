package org.poseestimation.socketconnect.wearMesgReceiver;

import android.util.Log;

import org.poseestimation.socketconnect.RemoteConst;
import org.poseestimation.socketconnect.communication.CommunicationKey;
import org.poseestimation.videodecoder.GlobalStaticVariable;

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

/**
 * 用于接收wear心率

 */
public class WearMesgReceiver {

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 4, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ReceiveWearMesgThreadFactory(), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            throw new RejectedExecutionException();
        }
    });
    private static volatile boolean isOpen;


    public static void start(){
        isOpen = true;
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(RemoteConst.WEARMESG_RECEIVE_PORT);
                    while(isOpen){
                        Socket socket = serverSocket.accept();
                        threadPool.execute(new WearMesgParseRunnable(socket));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public static void open(){
        isOpen = true;
    }

    public static void close(){
        isOpen = false;
    }

    public static void finish()
    {
        threadPool.shutdown();
    }
    public static class WearMesgParseRunnable implements Runnable{
        Socket socket;

        public WearMesgParseRunnable(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream is = new DataInputStream(socket.getInputStream());
                byte[] bytes = new byte[1024];
                int i=0;
                while(true){
                    bytes[i] = (byte) is.read();
                    if (bytes[i] == -1) {
                        break;
                    }
                    if((char)bytes[i] != CommunicationKey.EOF.charAt(0)){
                        i++;
                    }else{
                        String data = new String(bytes, 0, i+1, Charset.defaultCharset()).replace(CommunicationKey.EOF, "");
                        Float hearBeatRatio=Float.parseFloat(data);
                        Log.d("", "HearBeatRatio:"+hearBeatRatio);
                        if(hearBeatRatio==-1.0f)
                        {
                            isOpen=false;
                        }
                        GlobalStaticVariable.Companion.set_NewestWearMesg_HeartBeartRatio(hearBeatRatio);
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


}
