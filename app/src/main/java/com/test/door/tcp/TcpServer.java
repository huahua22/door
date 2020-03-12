package com.test.door.tcp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {
  private static final String TAG = "tcpServer";
  private static TcpServer mSocketServer = null;
  private ServerSocket mServerSocket;
  private Socket clientScoket;
  private InputStream mInputStream;
  private boolean isConnect = false;
  private HandlerThread mHandlerThread;
  private Handler serverHandler;
  private OutputStream mOutputStream;
  private ServerReceiveThread serverReceiveThread;
  public static String address = null;

  private TcpServer() {
  }

  public static TcpServer getInstance() {
    if (mSocketServer == null) {
      synchronized (TcpServer.class) {
        mSocketServer = new TcpServer();
      }
    }
    return mSocketServer;
  }

  @SuppressLint("HandlerLeak")
  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        //connect error
        case -1:
          if (null != mServerReceiveListener) {
            mServerReceiveListener.onConnectFail();
            disconnect();
          }
          break;
        //receive data
        case 100:
          Bundle bundle = msg.getData();
          byte[] buffer = bundle.getByteArray("data");
          int size = bundle.getInt("size");
          if (null != mServerReceiveListener) {
            Log.d(TAG, "on data receive");
            mServerReceiveListener.onDataReceive(buffer, size);
          }
          break;
      }
    }
  };


  public void disconnect() {
    isConnect = false;
    try {
      if (mOutputStream != null) {
        mOutputStream.close();
      }
      if (mInputStream != null) {
        mInputStream.close();
      }
      if (clientScoket != null) {
        clientScoket.close();
        clientScoket = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (serverReceiveThread != null) {
      serverReceiveThread.interrupt();//not intime destory thread,so need a flag
    }
  }

  public void start() {
    ServerThread serverThread = new ServerThread();
    serverThread.start();
    Log.d(TAG, "server cmd thread start");
    mHandlerThread = new HandlerThread("serverSendThread");
    mHandlerThread.start();
    serverHandler = new Handler(mHandlerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        Log.d(TAG, "sendServer thread:" + Thread.currentThread().getName());
        super.handleMessage(msg);
        if (msg.what == 1) {
          byte[] data = (byte[]) msg.obj;
          try {
            mOutputStream = clientScoket.getOutputStream();
            mOutputStream.write(data);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    };
  }

  class ServerThread extends Thread {

    public void run() {
      try {
        mServerSocket = new ServerSocket(9000);
      } catch (IOException e) {
        e.printStackTrace();
      }
      while (true) {
        try {
          clientScoket = mServerSocket.accept();
          address = String.valueOf(clientScoket.getInetAddress().getHostAddress());
          mInputStream = clientScoket.getInputStream();
          isConnect = true;
          serverReceiveThread = new ServerReceiveThread();
          serverReceiveThread.start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  class ServerReceiveThread extends Thread {
    @Override
    public void run() {
      super.run();
      while (isConnect) {
        try {
          byte[] buf = new byte[1024];
          if (mInputStream == null)
            return;
          int len = mInputStream.read(buf);//null data -1 , zrd serial rule size default 10
          if (len > 0) {
            Message msg = new Message();
            msg.what = 100;
            Bundle bundle = new Bundle();
            bundle.putByteArray("data", buf);
            bundle.putInt("size", len);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
          }
          Log.i(TAG, "SocketThread read listening");
          //Thread.sleep(100);//log eof
        } catch (IOException e) {
          mHandler.sendEmptyMessage(-1);
          Log.e(TAG, "SocketThread read io exception = " + e.getMessage());
          e.printStackTrace();
          return;
        }
      }
    }

  }


  public void sendMsg(byte[] data) {
    Message msg = new Message();
    msg.what = 1;
    msg.obj = data;
    serverHandler.sendMessage(msg);
  }

  private OnServerReceiveListener mServerReceiveListener = null;

  public interface OnServerReceiveListener {

    public void onConnectFail();

    public void onDataReceive(byte[] buffer, int size);
  }

  public void setOnServerReceiveListener(
    OnServerReceiveListener dataReceiveListener) {
    mServerReceiveListener = dataReceiveListener;
  }
}
