package com.test.door;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.test.door.scoket.ClicenSockets;
import com.test.door.tcp.TcpClient;
import com.test.door.tcp.TcpServer;
import com.xwr.videocode.TcpPcm;
import com.xwr.videocode.VideoSurfaceView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static com.test.door.Constants.acceptVideoByte;
import static com.test.door.Constants.endingByte;
import static com.test.door.Constants.handUpVideoByte;
import static com.test.door.Constants.refuseVideoByte;
import static com.test.door.Constants.requestVideoByte;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TcpServer.OnServerReceiveListener {
  private static String TAG = "MainActivity";

  // Used to load the 'native-lib' library on application startup.
  static {
    System.loadLibrary("native-lib");
  }

  int i = 0;
  boolean calldest = false;
  boolean destCall = false;
  // 增强文本
    /*TextInputLayout textInputLayout;
    TextInputEditText textInputEditText;*/
  Button btnStart;
  Button btnStop;
  Button btnCall;
  Button btnGet;
  TextView tvTip;
  VideoSurfaceView mVideoView;
  int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
  RelativeLayout mRelativeLayout;
  EditText mEditText;
  private InetAddress myAddress = null;
  private DatagramSocket mSocket = null;
  private boolean isStart = false;
  DatagramSocket socket = null;
  private DatagramPacket receivePacket;
  // ip address RegExp
  // String s = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
  private static final int BUFFER_LENGTH = 320;
  private Thread cmdThread;
  private boolean isThreadRunning = false;
  String address;
  private ClicenSockets clicenSockets;
  // 接收 OK 状态
  private boolean isVideo = false;
  byte[] b = new byte[4];
  String dest_ip;
  @SuppressLint("HandlerLeak")
  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        case 1:
          address = (String) msg.obj;
          mVideoView.initSocket(address);
          mEditText.setVisibility(View.GONE);
          tvTip.setVisibility(View.GONE);
          mVideoView.startRecod();
          isStart = true;
          break;
        case 2:
          address = (String) msg.obj;
          mVideoView.initSocket(address);
          sendCmd();  // 语音通话
          break;
        case 3:
          Log.i("mHandler", "用户繁忙！！！");
          break;
        case 4:
          Log.i("mHandler", "无法接通！！！");
          break;
        case 5:
          if (isVideo) {
            address = (String) msg.obj;
            mVideoView.initSocket(address);
            mEditText.setVisibility(View.GONE);
            tvTip.setVisibility(View.GONE);
            mVideoView.startRecod();
          } else {
            address = (String) msg.obj;
            mVideoView.initSocket(address);
            sendCmd();  // 语音通话
          }
          break;
        default:
          break;
      }
    }
  };

  {
    //    clicenSockets = new ClicenSockets();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    requestPermission();
    //       String ip = roomIdGetIP(1,1);

    // superEditText.setDividerDrawable(getResources().getDrawable(android.R.drawable.divider_horizontal_textfield));
    btnStart = (Button) findViewById(R.id.btn_start);
    btnStop = findViewById(R.id.btn_stop);
    btnCall = findViewById(R.id.btn_call);
    btnGet = findViewById(R.id.getIp);
    tvTip = findViewById(R.id.tvTip);
    mEditText = findViewById(R.id.ip_address);
    btnStart.setOnClickListener(this);
    btnStop.setOnClickListener(this);
    btnCall.setOnClickListener(this);
    btnGet.setOnClickListener(this);
    mRelativeLayout = findViewById(R.id.main);
    mVideoView = new VideoSurfaceView(this, mCameraId);
    mRelativeLayout.addView(mVideoView);
    TcpServer.getInstance().start();
    TcpServer.getInstance().setOnServerReceiveListener(this);
    // startSocket();
    /*接收数据线程*/
    // new Thread(new Receives(mHandler)).start();
    /**开启线程 命令*/
    // new Thread(new SendUDP()).start();
    //    clicenSockets.getThread_receive(mHandler);
    TextWatcher textWatcher = new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        if (mEditText.getText().length() > 4) {
          mEditText.setText("");
        }
        Log.i(TAG, mEditText.getText().toString());
      }
    };
    mEditText.addTextChangedListener(textWatcher);

  }


  private void callVideo() {
    isVideo = true;
    tvTip.setVisibility(View.GONE);
    if (mEditText.getText().toString().isEmpty()) {
      showToast("please input roomId");
    } else {
      Log.i("Mainactivity", "视频按钮被点击了，，，");
      TcpClient.getInstance().connect(address, 9000);
      btnCall.setVisibility(View.GONE);
      mRelativeLayout.setVisibility(View.VISIBLE);
      // SendUDP.ip = mEditText.getText().toString();


      Log.d(TAG, "click start btn");
      mEditText.setVisibility(View.GONE);
      //            mVideoView.startRecod();  // start 开启视频录像 录音
      // isStart = true;
      btnStart.setClickable(false);
      TcpClient.getInstance().setOnDataReceiveListener(dataReceiveListener);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_start:
        callVideo();
        break;
      case R.id.btn_stop:
        btnCall.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);

        if (TcpClient.getInstance().isConnect()) {
          TcpClient.getInstance().sendByteCmd(handUpVideoByte, 1001);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          TcpClient.getInstance().disconnect();
        }
        if (destCall) {
          TcpServer.getInstance().sendMsg(handUpVideoByte);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          TcpServer.getInstance().disconnect();
        }
        mVideoView.stopRecord();
        tvTip.setVisibility(View.GONE);
        btnStart.setClickable(true);
        btnCall.setClickable(true);
        mRelativeLayout.setVisibility(View.GONE);
                /*// 挂断时状态置为 不忙
                Receives.isBusy = false;*/
        break;
      case R.id.btn_call:
        isVideo = false;
        if (mEditText.getText().toString().isEmpty()) {
          Toast.makeText(this, "please input address", Toast.LENGTH_SHORT).show();
        } else {
                    /*if( !ipVerify(s,mEditText.getText().toString()) ){
                        showToast("请输入正确 IP 地址！");
                        return;
                    }*/
          btnStart.setVisibility(View.GONE);
          // sendCmd();
          tvTip.setVisibility(View.VISIBLE);
          mEditText.setVisibility(View.GONE);
          btnCall.setClickable(false);
        }
        break;
      case R.id.getIp:
        Log.i("setOnClickListener", roomIdGetIP(1, 1));
        Toast.makeText(MainActivity.this, "makeText" + roomIdGetIP(1, 1), Toast.LENGTH_SHORT).show();
        break;
    }
  }

  /**
   * @param RegExp   正则表达式
   * @param checkout 校对的字符
   * @return boolean
   * @description 正则表达式校验
   * @time 2020/1/8 15:29
   */
  private boolean ipVerify(String RegExp, String checkout) {
    Pattern pattern = Pattern.compile(RegExp);    // 编译正则表达式
    Matcher matcher = pattern.matcher(checkout);    // 创建给定输入模式的匹配器
    boolean bool = matcher.matches();
    return bool;
  }

  private void showToast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }


  private void startSocket() {
    if (socket != null)
      return;
    try {
      socket = new DatagramSocket(9000);
      if (receivePacket == null) {
        byte[] data = new byte[320];
        receivePacket = new DatagramPacket(data, BUFFER_LENGTH);
      }
      startSocketThread();
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  private void receiveMessage() {
    while (isThreadRunning) {
      if (socket != null) {
        try {
          socket.receive(receivePacket);
        } catch (IOException e) {
          Log.e(TAG, "UDP数据包接收失败！线程停止");
          e.printStackTrace();
          return;
        }
      }
      if (receivePacket == null || receivePacket.getLength() == 0) {
        Log.e(TAG, "无法接收UDP数据或者接收到的UDP数据为空");
        continue;
      }
      Log.d(TAG, " from " + receivePacket.getAddress().getHostAddress() + " length:" + receivePacket.getData().length);
      String recvData = new String(receivePacket.getData());
      Log.d(TAG, "rec data:" + recvData);
      if (recvData.indexOf("connect") != -1) {
        String message = "success";
        byte[] info = message.getBytes();
        DatagramPacket sendPacket = null;// 创建发送类型的数据报：  
        try {
          sendPacket = new DatagramPacket(info, info.length, InetAddress.getByName(receivePacket.getAddress().getHostAddress()), 9000);
          socket.send(sendPacket);
        } catch (UnknownHostException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (recvData != null && recvData.length() > 0) {
        Message msg = new Message();
        msg.what = 1;
        msg.obj = receivePacket.getAddress().getHostAddress();
        mHandler.sendMessage(msg);
        isThreadRunning = false;
      }
      if (receivePacket != null) {
        receivePacket.setLength(BUFFER_LENGTH);
      }
    }
  }

  private void startSocketThread() {
    isThreadRunning = true;
    cmdThread = new Thread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "rec thread is running...");
        receiveMessage();
      }
    });
    cmdThread.start();
  }

  void sendCmd() {
    if (socket == null) {
      startSocket();
    }
    new Thread() {
      @Override
      public void run() {
        super.run();
        String message = "connect";
        byte[] configInfo = message.getBytes();
        InetAddress ip = null; //即目的IP
        try {
          ip = InetAddress.getByName(mEditText.getText().toString());
        } catch (UnknownHostException e) {
          e.printStackTrace();
        }
        while (isThreadRunning) {
          DatagramPacket sendPacket = new DatagramPacket(configInfo, configInfo.length, ip, 9000);// 创建发送类型的数据报：  
          try {
            socket.send(sendPacket);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        Log.d(TAG, "stop thread");

      }
    }.start();
  }


  void requestPermission() {
    final int REQUEST_CODE = 1;
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{
          Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
        REQUEST_CODE);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (isStart) {
      mVideoView.stopRecord();
    }
    mVideoView.closeVideo();
    TcpServer.getInstance().disconnect();
    mHandler.removeCallbacks(cmdThread);
  }

  // 监听键盘事件
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    Log.i("onKeyDown keyCode", keyCode + "");
    return super.onKeyDown(keyCode, event);
  }


  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    int floor = 0;
    int room = 0;
    Log.e("onKeyUp keyCode", keyCode + "");
    String roomIdFix = "";
    if (keyCode == 136) {
      if (mEditText.getText().length() < 3) {
        showToast("输入完整的房间号");
        return false;
      }
      String roomIdstr = mEditText.getText().toString();
      int index = roomIdstr.indexOf("0");
      Log.e("index", index + "");
      if (index == 0) {
        String newstr = roomIdstr.substring(1);
        Log.d(TAG, "newStr:" + newstr);
        if (newstr.indexOf("0") == 0) {
          showToast("输入房间号错误");
          mEditText.getText().clear();
          return false;
        }
        Log.i("newstr", newstr);
        roomIdFix = newstr;
      } else {
        roomIdFix = roomIdstr;
      }
      Log.i("roomIdFix", roomIdFix);
      // callVideo();
      //            int i = Integer.valueOf(roomIdFix);
      //            showToast("输入的房间号是："+roomIdByInput);
      if (roomIdFix.length() == 3) {
        floor = Integer.valueOf(roomIdFix.substring(0, 1));
        room = Integer.valueOf(roomIdFix.substring(1, 3));
      } else if (roomIdFix.length() == 4) {
        floor = Integer.valueOf(roomIdFix.substring(0, 2));
        room = Integer.valueOf(roomIdFix.substring(2, 4));
      } else if (roomIdFix.length() == 2) {
        showToast("输入房间号有误");
        return false;
      }
      dest_ip = roomIdGetIP(floor, room);
      Log.d(TAG, dest_ip);
      Toast.makeText(MainActivity.this, "dest_ip:" + dest_ip, Toast.LENGTH_SHORT).show();
      //            mEditText.setText(dest_ip);
      //      ClicenSockets.targetIp = dest_ip;
      //      address = "192.168.0.200";
      address = dest_ip;
      callVideo();

    }
    return super.onKeyUp(keyCode, event);
  }


  public native String stringFromJNI();

  public native String roomIdGetIP(int floor, int roomId);

  private TcpClient.OnDataReceiveListener dataReceiveListener = new TcpClient.OnDataReceiveListener() {
    @Override
    public void onConnectSuccess() {
      Log.i(TAG, "onDataReceive connect success");
      TcpClient.getInstance().sendByteCmd(requestVideoByte, 1001);
      calldest = true;
    }

    @Override
    public void onConnectFail() {
      Log.e("huahua", "onDataReceive connect fail");
      //      showToast("连接失败");
      if (isStart) {
        mVideoView.stopRecord();
        isStart = false;
        VisiableView();
      }
    }

    @Override
    public void onDataReceive(byte[] buffer, int size, int requestCode) {
      //获取有效长度的数据
      byte[] data = new byte[size];
      System.arraycopy(buffer, 0, data, 0, size);
      if (data[1] == 0x03) {
        Log.d(TAG, "video");
        showToast("视频通话");
        Log.d(TAG, "server video address:" + address);
        mVideoView.initSocket(address);
        mVideoView.startRecod();
        isStart = true;
      }
      if (data[1] == 0x02) {
        showToast("挂断");
        TcpClient.getInstance().disconnect();
        mVideoView.stopRecord();
        VisiableView();
      }
      if (buffer[1] == 0x07) {
        showToast("已拒绝");
        TcpClient.getInstance().disconnect();
        VisiableView();

      }
    }
  };

  @Override
  public void onConnectFail() {
    Log.d(TAG, "Fail");
    //    showToast("连接失败");
    TcpServer.getInstance().disconnect();
    if (isStart) {
      mVideoView.stopRecord();
      isStart = false;
      VisiableView();
    }
  }

  @Override
  public void onDataReceive(byte[] buffer, int size) {
    Log.d(TAG, "server size:" + size);
    if (buffer[1] == 0x01) {
      showToast("视频通话");
      setDialog();
    }
    if (buffer[1] == 0x02) {
      showToast("对方已挂断");
      TcpServer.getInstance().sendMsg(endingByte);
      mVideoView.stopRecord();
      isStart = false;
      VisiableView();
    }
  }

  private void setDialog() {
    final AlertDialog alertDialog2 = new AlertDialog.Builder(this)
      .setTitle("提示")
      .setMessage("用户请求视频，是否接听")
      .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          TcpServer.getInstance().sendMsg(acceptVideoByte);
          if (TcpServer.address != null) {
            inVisiableView();
            mVideoView.initSocket(TcpServer.address);
            mVideoView.startRecod();
            destCall = true;
          }
          dialogInterface.dismiss();
        }
      })

      .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {//添加取消
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          TcpServer.getInstance().sendMsg(refuseVideoByte);
          TcpServer.getInstance().disconnect();
          dialogInterface.dismiss();
        }
      }).create();
    alertDialog2.show();
  }

  void VisiableView() {
    btnCall.setVisibility(View.VISIBLE);
    mEditText.setVisibility(View.VISIBLE);
    tvTip.setVisibility(View.GONE);
    btnCall.setClickable(true);
    mRelativeLayout.setVisibility(View.GONE);
  }

  void inVisiableView() {
    btnCall.setVisibility(View.GONE);
    mRelativeLayout.setVisibility(View.VISIBLE);
    mEditText.setVisibility(View.GONE);
    btnStart.setClickable(false);
  }


}
