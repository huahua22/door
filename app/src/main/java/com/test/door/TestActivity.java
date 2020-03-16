package com.test.door;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.test.door.tcp.TcpClient;
import com.test.door.tcp.TcpServer;
import com.xwr.videocode.VideoCameraView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static com.test.door.Constants.handUpVideoByte;
import static com.test.door.Constants.requestVideoByte;

public class TestActivity extends AppCompatActivity implements View.OnClickListener, TcpServer.OnServerReceiveListener {

  Button btnStop;
  Button btnVideo;
  VideoCameraView mVideoCameraView;
  int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
  RelativeLayout mRelativeLayout;
  private boolean isStart = false;
  String address;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test);
    requestPermission();
    initView();
  }

  private void initView() {
    btnStop = findViewById(R.id.btn_stop);
    btnVideo = findViewById(R.id.video);
    btnStop.setOnClickListener(this);
    btnVideo.setOnClickListener(this);
    mRelativeLayout = findViewById(R.id.main);
    mVideoCameraView = new VideoCameraView(this, mCameraId);
    mRelativeLayout.addView(mVideoCameraView);
    TcpServer.getInstance().start();
    TcpServer.getInstance().setOnServerReceiveListener(this);
  }


  private void callVideo() {
//    TcpClient.getInstance().connect(address, 9000);
    mRelativeLayout.setVisibility(View.VISIBLE);
    mVideoCameraView.startRecod();
//    TcpClient.getInstance().setOnDataReceiveListener(dataReceiveListener);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_stop:
        if (TcpClient.getInstance().isConnect()) {
          TcpClient.getInstance().sendByteCmd(handUpVideoByte, 1001);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          TcpClient.getInstance().disconnect();
        }
        mVideoCameraView.stopRecord();
        mRelativeLayout.setVisibility(View.GONE);
        break;
      case R.id.video:
        address = "192.168.4.210";
        callVideo();
        break;
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
      mVideoCameraView.stopRecord();
    }
    mVideoCameraView.closeVideo();
    TcpServer.getInstance().disconnect();
  }

  private TcpClient.OnDataReceiveListener dataReceiveListener = new TcpClient.OnDataReceiveListener() {
    @Override
    public void onConnectSuccess() {
      TcpClient.getInstance().sendByteCmd(requestVideoByte, 1001);
    }

    @Override
    public void onConnectFail() {
      Log.e("tcpClient", "onDataReceive connect fail");
      if (isStart) {
        mVideoCameraView.stopRecord();
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
        showToast("视频通话");
        mVideoCameraView.initSocket(address);
        mVideoCameraView.startRecod();
        isStart = true;
      }
      if (data[1] == 0x02) {
        showToast("挂断");
        TcpClient.getInstance().disconnect();
        mVideoCameraView.stopRecord();
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
    TcpServer.getInstance().disconnect();
    if (isStart) {
      mVideoCameraView.stopRecord();
      isStart = false;
      VisiableView();
    }
  }

  @Override
  public void onDataReceive(byte[] buffer, int size) {
    if (buffer[1] == 0x01) {
      showToast("视频通话");
    }
    if (buffer[1] == 0x02) {
      showToast("对方已挂断");
      if (isStart) {
        mVideoCameraView.stopRecord();
        isStart = false;
        VisiableView();
      }
    }
    if (buffer[1] == 0x07 && buffer[2] == 0x03) {
      TcpServer.getInstance().disconnect();
    }
  }


  void VisiableView() {
    mRelativeLayout.setVisibility(View.GONE);
  }

  void inVisiableView() {
    mRelativeLayout.setVisibility(View.VISIBLE);
  }


}
