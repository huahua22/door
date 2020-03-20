package com.test.door;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
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

import com.test.door.tcp.TcpClient;
import com.test.door.tcp.TcpServer;
import com.xwr.videocode.VideoSurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static com.test.door.Constants.acceptVideoByte;
import static com.test.door.Constants.handUpVideoByte;
import static com.test.door.Constants.refuseVideoByte;
import static com.test.door.Constants.requestVideoByte;
import static com.test.door.IpServer.roomIdGetIP;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TcpServer.OnServerReceiveListener {
  private static String TAG = "MainActivity";
  AlertDialog alertDialog2;
  boolean calldest = false;
  boolean destCall = false;

  Button btnStart;
  Button btnStop;
  Button btnCall;
  Button btnGet;
  Button btnVideo;
  TextView tvTip;
  VideoSurfaceView mVideoView;
  int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
  RelativeLayout mRelativeLayout;
  EditText mEditText;
  private boolean isStart = false;
  String address;
  // 接收 OK 状态
  private boolean isVideo = false;
  byte[] b = new byte[4];
  String dest_ip;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    requestPermission();
    initView();
  }

  private void initView() {
    btnStart = (Button) findViewById(R.id.btn_start);
    btnStop = findViewById(R.id.btn_stop);
    btnCall = findViewById(R.id.btn_call);
    btnGet = findViewById(R.id.getIp);
    tvTip = findViewById(R.id.tvTip);
    btnVideo = findViewById(R.id.video);
    mEditText = findViewById(R.id.ip_address);
    btnStart.setOnClickListener(this);
    btnStop.setOnClickListener(this);
    btnCall.setOnClickListener(this);
    btnGet.setOnClickListener(this);
    btnVideo.setOnClickListener(this);
    mRelativeLayout = findViewById(R.id.main);
    mVideoView = new VideoSurfaceView(this, mCameraId);
    mRelativeLayout.addView(mVideoView);
    TcpServer.getInstance().start();
    TcpServer.getInstance().setOnServerReceiveListener(this);
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
      TcpClient.getInstance().connect(address, 9000);
      btnCall.setVisibility(View.GONE);
      mRelativeLayout.setVisibility(View.VISIBLE);
      mEditText.setVisibility(View.GONE);
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
        break;
      case R.id.btn_call:
        isVideo = false;
        if (mEditText.getText().toString().isEmpty()) {
          Toast.makeText(this, "please input address", Toast.LENGTH_SHORT).show();
        } else {
          btnStart.setVisibility(View.GONE);
          tvTip.setVisibility(View.VISIBLE);
          mEditText.setVisibility(View.GONE);
          btnCall.setClickable(false);
        }
        break;
      case R.id.getIp:
        Log.i("setOnClickListener", roomIdGetIP(1, 1));
        Toast.makeText(MainActivity.this, "makeText" + roomIdGetIP(1, 1), Toast.LENGTH_SHORT).show();
        break;
//      case R.id.video:
//        address = "192.168.4.210";
//        callVideo();
//        break;
    }
  }

  private void showToast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
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
      mEditText.setText(dest_ip);
      address = dest_ip;
      callVideo();

    }
    return super.onKeyUp(keyCode, event);
  }


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


  /*
   *服务端接收消息监听
   */
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
      if (isStart) {
        mVideoView.stopRecord();
        isStart = false;
        VisiableView();
      }
      if (alertDialog2 != null) {
        alertDialog2.dismiss();
      }

    }
    if (buffer[1] == 0x07 && buffer[2] == 0x03) {
      TcpServer.getInstance().disconnect();
    }
  }

  private void setDialog() {
    alertDialog2 = new AlertDialog.Builder(this)
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
          dialogInterface.dismiss();
        }
      }).create();
    alertDialog2.show();
  }

  void VisiableView() {
    btnCall.setVisibility(View.VISIBLE);
    mEditText.setVisibility(View.VISIBLE);
    tvTip.setVisibility(View.GONE);
    btnCall.setClickable(false);
    mRelativeLayout.setVisibility(View.GONE);
  }

  void inVisiableView() {
    btnCall.setVisibility(View.GONE);
    mRelativeLayout.setVisibility(View.VISIBLE);
    mEditText.setVisibility(View.GONE);
    btnStart.setClickable(false);
  }


}
