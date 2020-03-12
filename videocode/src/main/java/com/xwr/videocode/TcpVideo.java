package com.xwr.videocode;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TcpVideo {
  private static final String TAG = "tcpVideo";
  private static TcpVideo mSocketVideo = null;
  private Socket mSocket;
  private SocketThread mSocketThread;
  private OutputStream mOutputStream;
  private InputStream mInputStream;
  private HandlerThread mHandlerThread;
  private Handler videoHandler;
  private byte[] recv = new byte[2];

  private void init() {
    mHandlerThread = new HandlerThread("videoThread");
    mHandlerThread.start();
    videoHandler = new Handler(mHandlerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        Log.d(TAG, "sendVideo thread:" + Thread.currentThread().getName());
        super.handleMessage(msg);
        byte[] data = (byte[]) msg.obj;
        try {
          if (mOutputStream != null) {
            mOutputStream.write(data);
            mOutputStream.flush();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  private TcpVideo() {
  }

  public static TcpVideo getInstance() {
    if (mSocketVideo == null) {
      synchronized (TcpVideo.class) {
        mSocketVideo = new TcpVideo();
      }
    }
    return mSocketVideo;
  }

  public void connect(String ip) {
    mSocketThread = new SocketThread(ip);
    mSocketThread.start();
    init();
  }

  private class SocketThread extends Thread {

    private String ip;
    private int port = 9001;

    public SocketThread(String ip) {
      this.ip = ip;
    }

    @Override
    public void run() {
      super.run();
      //connect ...
      try {
        if (mSocket != null) {
          mSocket.close();
          mSocket = null;
        }

        InetAddress ipAddress = InetAddress.getByName(ip);
        mSocket = new Socket(ipAddress, port);

        //设置不延时发送
        //mSocket.setTcpNoDelay(true);
        //设置输入输出缓冲流大小
        //mSocket.setSendBufferSize(8*1024);
        //mSocket.setReceiveBufferSize(8*1024);

        if (isConnect()) {
          mOutputStream = mSocket.getOutputStream();
          mInputStream = mSocket.getInputStream();
          Log.d(TAG, "connect success");
        } else {
          Log.e(TAG, "SocketThread connect fail");
          return;
        }
      } catch (IOException e) {
        Log.e(TAG, "SocketThread connect io exception = " + e.getMessage());
        e.printStackTrace();
        return;
      }
      Log.d(TAG, "SocketThread connect over ");
    }
  }

  public boolean isConnect() {
    boolean flag = false;
    if (mSocket != null) {
      flag = mSocket.isConnected();
    }
    return flag;
  }

  public void sendImage(byte[] data) {
    //    byte[] result = new byte[1452];
    //    int destPos = 0;
    //    int size;
    //    if (length > 1452) {
    //      if (length % 1452 > 0) {
    //        size = (length / 1452) + 1;
    //      } else {
    //        size = length / 1;
    //      }
    //      Log.d(TAG, "size:" + size);
    //      for (int i = 0; i < size; i++) {
    //        Arrays.fill(result, (byte) 0);
    //        System.arraycopy(data, destPos, result, 0, 1452);
    //        destPos += 1452;
    //        Message msg = new Message();
    //        msg.obj = result;
    //        videoHandler.sendMessageDelayed(msg, 1000);
    //      }
    //    } else {
    //      Message msg = new Message();
    //      msg.obj = data;
    //      videoHandler.sendMessageDelayed(msg, 1000);
    //    }
    Message msg = new Message();
    msg.obj = data;
    videoHandler.sendMessage(msg);

  }


  public void close() {
    try {
      if (mOutputStream != null) {
        mOutputStream.flush();
        mOutputStream.close();
      }
      if (mSocket != null) {
        mSocket.close();
        mSocket = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    mHandlerThread.quit();
    if (mSocketThread != null) {
      mSocketThread.interrupt();//not intime destory thread,so need a flag
    }
  }
}
