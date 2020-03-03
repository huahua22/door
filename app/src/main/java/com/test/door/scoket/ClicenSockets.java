package com.test.door.scoket;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.test.door.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClicenSockets{

    String TAG = "ClicenSokets";
    private Handler mHandler;
    public static String targetIp = "";
    private int targetPort = 9000;
    public static int MAX_DATA_PACKET_LENGTH = 4;
    private static DatagramSocket clicenSocket;
    private static DatagramPacket clicenPacket;
    private DatagramPacket clicenrecPacket;
    static byte[] b = new byte[MAX_DATA_PACKET_LENGTH];
    static {
        try {
            if (clicenSocket == null){
                clicenSocket = new DatagramSocket();
            }
            clicenPacket = new DatagramPacket(b, MAX_DATA_PACKET_LENGTH);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public ClicenSockets(){}
    /**
     * @brief 发送线程
     */
    public void getThread_Send(byte[] data){
        Log.i(TAG,"getThread_Send");
        new Thread(new Thread_Send(data)).start();
    }

    public class Thread_Send implements Runnable {
        //发送数据包
        private DatagramPacket Packet_Send;
        /**
         *  * @brief 构造函数
         *  * @param data:需要发送的数据
         *  * @param len:数据字节数据
         * */
        public Thread_Send(byte[] data) {
            clicenPacket.setData(data);
        }
        @Override
        public void run() {
            Log.i(TAG, "Thread_Send run()");
            try {
                clicenPacket.setPort(targetPort);
                clicenPacket.setAddress(InetAddress.getByName(targetIp));
                clicenSocket.send(clicenPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void getThread_receive(Handler mHandler){
        Log.i(TAG,"getThread_receive");
        new Thread(new Thread_receive(mHandler)).start();
    }
    class Thread_receive implements Runnable{
        private Handler mHanler;
        public Thread_receive(Handler h){
           mHandler = h;
        }
        @Override
        public void run() {
            if (clicenSocket == null){
                try {
                    clicenSocket = new DatagramSocket();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
            while (true){
                Log.i(TAG,"Thread_receive run(),等待接收！");
                try {
                    clicenrecPacket = new DatagramPacket(new byte[4],4);
                    clicenSocket.receive(clicenrecPacket);
                    Log.i(TAG,"收到"+clicenrecPacket.getLength() + " "+ clicenrecPacket.getData()[0]+clicenrecPacket.getData()[1]+clicenrecPacket.getData()[2]+clicenPacket.getData()[3]);
                    if(clicenrecPacket.getLength() == 0 ){
                        clicenrecPacket = null;
                        continue;}
                    //serverSocket.send();
                    int udpCMD = clicenrecPacket.getData()[0];
                    Log.i(TAG, "ClicenSockets 接收命令" + udpCMD);
                    if (Constants.OK == udpCMD){
                        Message msg = new Message();
                        msg.what = 5;
                        msg.obj = clicenrecPacket.getAddress().getHostAddress();
                        mHandler.sendMessage(msg);
                    }
                    clicenrecPacket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}