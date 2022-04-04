package org.poseestimation.socketconnect.communication.slave;

import android.util.Log;

import org.poseestimation.socketconnect.RemoteConst;
import org.poseestimation.socketconnect.communication.CommunicationKey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FrameDataSender {
    private static ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(10, 10, 1,
                    TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new SendFrameDataThreadFactory(),
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            throw new RejectedExecutionException();
                        }
                    });


    public static void addFrameData(final FrameData frameData){
        addTask(new FrameDataSender.FrameDataRunnable(frameData));
    }

    private static void addTask(FrameDataSender.FrameDataRunnable runnable){
        try{
            threadPool.execute(runnable);
        }catch (RejectedExecutionException e){
            e.printStackTrace();
            if(runnable.frameData.getCallback()!=null){
                runnable.frameData.getCallback().onError("frameData is rejected");
            }
        }
    }
    private static class FrameDataRunnable implements Runnable {

        FrameData frameData;

        public FrameDataRunnable(FrameData frameData) {
            this.frameData = frameData;
        }

        @Override
        public void run() {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(frameData.getDestIp(), RemoteConst.FRAME_RECEIVE_PORT));
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                //发送命令内容
                os.write(frameData.getContent());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
