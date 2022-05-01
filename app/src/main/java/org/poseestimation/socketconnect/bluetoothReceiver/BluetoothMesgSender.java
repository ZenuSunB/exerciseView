package org.poseestimation.socketconnect.bluetoothReceiver;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.poseestimation.socketconnect.communication.CommunicationKey;
import org.poseestimation.socketconnect.communication.host.SendCommandThreadFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BluetoothMesgSender {
    static UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(2, 2, 1,
                    TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new SendCommandThreadFactory(),
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            throw new RejectedExecutionException();
                        }
                    });


    public static void addBluetoothMesg(final BluetoothMesg bluetoothMesg){
        addTask(new BluetoothMesgSender.BluetoothMesgRunnable(bluetoothMesg));
    }

    private static void addTask(BluetoothMesgSender.BluetoothMesgRunnable runnable){
        try{
            threadPool.execute(runnable);
        }catch (RejectedExecutionException e){
            e.printStackTrace();
        }
    }
    private static class BluetoothMesgRunnable implements Runnable{

        BluetoothMesg bluetoothMesg;
        public BluetoothMesgRunnable(BluetoothMesg bluetoothMesg){
            this.bluetoothMesg = bluetoothMesg;
        }
        @Override
        public void run() {
            BluetoothSocket  socket=null;
            try {
                socket  = BluetoothMesg.getBluetoothDevice().createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                OutputStream os = socket.getOutputStream();
                if(os!=null) {
                    //发送命令内容
                    Log.d("TAG", "run: 111111111111111");
                    os.write(bluetoothMesg.getContent());
                    os.write(CommunicationKey.EOF.getBytes());
                    Log.d("TAG", "run: 222222222222222");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }catch (SecurityException e){
                e.printStackTrace();
            }
            finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
