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

public class TcpPcm {
  private static final String TAG = "tcpPcm";
  private static TcpPcm mSocketClient = null;
  private HandlerThread mHandlerThread;
  Handler pcmHandler;


  public static TcpPcm getInstance() {
    if (mSocketClient == null) {
      synchronized (TcpPcm.class) {
        mSocketClient = new TcpPcm();
      }
    }
    return mSocketClient;
  }


  String TAG_log = "Socket_Pcm";
  private Socket mSocket;

  private OutputStream mOutputStream;
  private InputStream mInputStream;

  private SocketThread mSocketThread;
  private boolean isStop = false;//thread flag

  private void init() {
    mHandlerThread = new HandlerThread("pcmThread");
    mHandlerThread.start();
    pcmHandler = new Handler(mHandlerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        Log.d(TAG, "sendPCm thread:" + Thread.currentThread().getName());
        super.handleMessage(msg);
        byte[] data = (byte[]) msg.obj;
        Log.d(TAG, "pcm length:" + data.length);
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

  private class SocketThread extends Thread {

    private String ip;
    private int port;


    public SocketThread(String ip, int port) {
      this.ip = ip;
      this.port = port;
    }


    @Override
    public void run() {
      Log.d(TAG_log, "SocketThread start ");
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
          //          mHandlerThread.start();
          isStop = false;
        } else {
          Log.e(TAG_log, "SocketThread connect fail");
          return;
        }

      } catch (IOException e) {
        Log.e(TAG_log, "SocketThread connect io exception = " + e.getMessage());
        e.printStackTrace();
        return;
      }
      Log.d(TAG_log, "SocketThread connect over ");

      //read ...
      while (isConnect() && !isStop && !isInterrupted()) {
        int size;
        try {
          byte[] buffer = new byte[320];
          if (mInputStream == null)
            return;
          size = mInputStream.read(buffer);//null data -1 , zrd serial rule size default 10
          if (size > 0) {
            AudioTrackManager.getInstance().startPlay(buffer);
          }
        } catch (IOException e) {
          Log.e(TAG_log, "SocketThread read io exception = " + e.getMessage());
          e.printStackTrace();
          return;
        }
      }
    }
  }


  //==============================socket connect============================

  public void connect(String ip) {
    mSocketThread = new SocketThread(ip, 9002);
    mSocketThread.start();
    init();

  }

  /**
   * socket is connect
   */
  public boolean isConnect() {
    boolean flag = false;
    if (mSocket != null) {
      flag = mSocket.isConnected();
    }
    return flag;
  }

  /**
   * socket disconnect
   */
  public void disconnect() {
    isStop = true;
    try {
      if (mOutputStream != null) {
        mOutputStream.close();
      }

      if (mInputStream != null) {
        mInputStream.close();
      }

      if (mSocket != null) {
        mSocket.close();
        mSocket = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    AudioTrackManager.getInstance().pausePlay();
    AudioRecordManager.getInstance().stopRecording();
    if (mHandlerThread != null) {
      mHandlerThread.quit();
    }
    //    mHandlerThread.quit();
    if (mSocketThread != null) {
      mSocketThread.interrupt();//not intime destory thread,so need a flag
    }

  }


  /**
   * send byte[] cmd
   * Exception : android.os.NetworkOnMainThreadException
   */
  public void sendBytePcm(byte[] data) {
    Message msg = new Message();
    msg.obj = data;
    pcmHandler.sendMessage(msg);
  }
}
