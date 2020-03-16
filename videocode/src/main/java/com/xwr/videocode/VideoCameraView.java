package com.xwr.videocode;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xwr.speex.SpeexUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import static com.xwr.videocode.CameraFormat.determineMaximumSupportedFramerate;

/**
 * Create by xwr on 2020/3/16
 * Describe:
 */
public class VideoCameraView extends SurfaceView implements SurfaceHolder.Callback {
  private static String TAG = "VideoView";
  private SurfaceHolder mSurfaceHolder;
  private Camera mCamera;
  private int mCameraId;
  int width = 640;
  int height = 480;
  int framerate = 10;
  int bitrate;
  Context mContext;
  //定义摄像机
  private Camera camera;
  private String path = FileUtil.getSDPath() + "/test.h264";
  AvcEncoder mEncoder;
  private static int yuvqueuesize = 10;

  //待解码视频缓冲队列，静态成员！
  public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);

  //构造函数
  public VideoCameraView(Context context, int cameraId) {
    super(context);
    //获取Holder
    mContext = context;
    mCameraId = cameraId;
    Log.d(TAG, "camera:" + cameraId);
    mSurfaceHolder = getHolder();
    mSurfaceHolder.addCallback(this);
    mSurfaceHolder.setFixedSize(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
    bitrate = 2 * width * height * framerate / 20;
    mEncoder = new AvcEncoder(width, height, framerate, bitrate);
    Log.d("huahua", "path:" + path);
    FileUtil.createFile(path);
  }

  public void initSocket(String address) {
    TcpVideo.getInstance().connect(address);

  }


  /**
   * 开始录音
   */
  private void startRecordVoice() {
    SpeexUtil.getInstance().init();
    AudioRecordManager.getInstance().startRecording(new AudioRecordManager.OnAudioRecordListener() {
      @Override
      public void onVoiceRecord(final byte[] data, int size) {
        TcpPcm.getInstance().sendBytePcm(data);
      }
    });
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    //开启摄像机
    boolean isCreate = createCamera(surfaceHolder);
    Log.d(TAG, "isCreate:" + isCreate);
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    //关闭摄像机
    stopCamera();
    destroyCamera();
  }


  //关闭摄像机
  private void stopCamera() {
    if (camera != null) {
      camera.setPreviewCallback(null);
      camera.stopPreview();
      camera.release();
      camera = null;
    }
  }


  private boolean createCamera(SurfaceHolder surfaceHolder) {
    if (mCamera != null) {
      mCamera.release();
      mCamera = null;
    }
    try {
      Log.d(TAG, "create camera");
      mCamera = Camera.open(mCameraId);
      Camera.Parameters parameters = mCamera.getParameters();
      int[] max = determineMaximumSupportedFramerate(parameters);
      Camera.CameraInfo camInfo = new Camera.CameraInfo();
      Camera.getCameraInfo(mCameraId, camInfo);
      //设置预览格式
      parameters.setPreviewFormat(ImageFormat.NV21);
      List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
      for (int i = 0; i < sizes.size(); i++) {
        Log.d(TAG, "camera support size:width--" + sizes.get(i).width + " height--" + sizes.get(i).height);
      }
      //设置预览图像分辨率
      parameters.setPreviewSize(width, height);
      parameters.setPreviewFpsRange(max[0], max[1]);
      mCamera.setParameters(parameters);
      mCamera.setPreviewDisplay(surfaceHolder);
      return true;
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      String stack = sw.toString();
      Log.e(TAG, stack);
      destroyCamera();
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 销毁Camera
   */
  protected synchronized void destroyCamera() {
    if (mCamera != null) {
      mCamera.stopPreview();
      try {
        mCamera.release();
      } catch (Exception e) {

      }
      mCamera = null;
    }
  }


  /**
   * 停止录制
   */
  public synchronized void stopRecord() {
    if (mCamera != null) {
      mCamera.setPreviewCallback(null);
      mCamera.setPreviewCallbackWithBuffer(null);
      mCamera.stopPreview();
    }
    TcpVideo.getInstance().close();
    mEncoder.StopThread();
  }


  /**
   * 关闭录制
   */
  public void closeVideo() {
    stopCamera();
    destroyCamera();
    if (TcpVideo.getInstance().isConnect()) {
      TcpVideo.getInstance().close();
    }
  }

  /**
   * 开启录制
   */
  public synchronized void startRecod() {
    if (mCamera != null) {
      mCamera.startPreview();
      int previewFormat = mCamera.getParameters().getPreviewFormat();
      Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
      int size = previewSize.width * previewSize.height
        * ImageFormat.getBitsPerPixel(previewFormat)
        / 8;
      mCamera.addCallbackBuffer(new byte[size]);
      mCamera.setPreviewCallbackWithBuffer(previewCallback);
      mEncoder.StartEncoderThread();
    } else {
      Log.d(TAG, "start fail");
    }
  }

  //定义Camera的回调方法
  private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
      synchronized (this) {
        if (data == null) {
          return;
        }
        putYUVData(data, data.length);
      }
    }

  };

  public void putYUVData(byte[] buffer, int length) {
    if (YUVQueue.size() >= 10) {
      YUVQueue.poll();
    }
    YUVQueue.add(buffer);
  }

}
