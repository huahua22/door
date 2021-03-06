package com.xwr.videocode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;

import static com.xwr.videocode.CameraFormat.determineMaximumSupportedFramerate;
import static com.xwr.videocode.CameraFormat.getDgree;
import static com.xwr.videocode.TypeConUtil.intToByteArray;

//import com.xwr.speex.SpeexUtil;


/**
 * Create by xwr on 2019/11/23
 * Describe:
 */
@SuppressLint("NewApi")
public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
  private static String TAG = "MyVideoView";
  private SurfaceHolder mSurfaceHolder;
  NV21Convertor mConvertor;
  MediaCodec mMediaCodec;
  private Camera mCamera;
  private IVideoRecoderListener mIVideoRecoderListener;
  private int mCameraId;
  int width = 640;
  int height = 480;
  int framerate = 10;
  int bitrate;
  Context mContext;
  //定义摄像机
  private Camera camera;
  private String path = FileUtil.getSDPath() + "/test.h264";
  private String pcm_path = FileUtil.getSDPath() + "/record.pcm";
  int frameSizeFlag = 0;

//  private Handler handler = new Handler();
//  private Runnable task = new Runnable() {
//    public void run() {
//      handler.postDelayed(this, 1000);
//      Log.d(TAG, "frameSize：" + frameSizeFlag);
//      framerate = 0;
//    }
//  };

  //构造函数
  public VideoSurfaceView(Context context, int cameraId) {
    super(context);
    //获取Holder
    mContext = context;
    mCameraId = cameraId;
    Log.d(TAG, "camera:" + cameraId);
    mSurfaceHolder = getHolder();
    initMediaCodec();
    mSurfaceHolder.addCallback(this);
    mSurfaceHolder.setFixedSize(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
    Log.d("huahua", "path:" + path);
    FileUtil.createFile(path);
    FileUtil.createFile(pcm_path);

  }

  public void initSocket(String address) {
    TcpVideo.getInstance().connect(address);
    //    TcpPcm.getInstance().connect(address);
  }


  /**
   * 开始录音
   */
  private void startRecordVoice() {
    //    SpeexUtil.getInstance().init();
    //    final short[] outdata = new short[320];
    AudioRecordManager.getInstance().startRecording(new AudioRecordManager.OnAudioRecordListener() {
      @Override
      public void onVoiceRecord(final byte[] data, int size) {
        TcpPcm.getInstance().sendBytePcm(data);
        FileUtil.save(data, 0, size, pcm_path, true);
        //        mPcmUdpUtil.sendMessage(data, dstAddress);
        //        mPcmUdpUtil.setUdpReceiveCallback(new PcmUdpUtil.OnUDPReceiveCallbackBlock() {
        //          @Override
        //          public void OnParserComplete(byte[] playdata) {
        //            SpeexUtil.getInstance().echoCancellation(toShortArray(data), toShortArray(playdata), outdata);
        //            Log.d("pcm", "length:" + outdata.length);
        //
        //          }
        //
        //        });
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
    //    AudioRecordManager.getInstance().stopRecording();
  }

  private void initMediaCodec() {
    Log.d(TAG, "init media codec");
    int dgree = getDgree(mContext);
    bitrate = 2 * width * height * framerate / 20;
    EncoderDebugger debugger = EncoderDebugger.debug(mContext, width, height);
    mConvertor = debugger.getNV21Convertor();
    try {
      mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
      MediaFormat mediaFormat;
      if (dgree == 0) {
        mediaFormat = MediaFormat.createVideoFormat("video/avc", height, width);
      } else {
        mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
      }
      mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
      mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
      mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        debugger.getEncoderColorFormat());
      mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);//关键帧间隔时间
      mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      mMediaCodec.start();
    } catch (IOException e) {
      e.printStackTrace();
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
      int cameraRotationOffset = camInfo.orientation;
      int rotate = (360 + cameraRotationOffset - getDgree(mContext)) % 360;
      parameters.setRotation(rotate);
      List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
      for (int i = 0; i < previewFormats.size(); i++) {
        Log.d(TAG, "formats:" + previewFormats.get(i));
      }
      parameters.setPreviewFormat(ImageFormat.NV21);
      List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
      for (int i = 0; i < sizes.size(); i++) {
        Log.d(TAG, "camera support size:width--" + sizes.get(i).width + " height--" + sizes.get(i).height);
      }
      parameters.setPreviewSize(width, height);
      parameters.setPreviewFpsRange(max[0], max[1]);
      mCamera.setParameters(parameters);
      // mCamera.autoFocus(null);
      int displayRotation;
      displayRotation = (cameraRotationOffset - getDgree(mContext) + 360) % 360;
      mCamera.setDisplayOrientation(displayRotation);
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
    //    TcpPcm.getInstance().disconnect();
    //    AudioTrackManager.getInstance().pausePlay();
    //    AudioRecordManager.getInstance().stopRecording();
  }


  /**
   * 关闭摄像
   */
  public void closeVideo() {
    stopCamera();
    destroyCamera();
    mMediaCodec.stop();
    mMediaCodec.release();
    mMediaCodec = null;
    if (TcpVideo.getInstance().isConnect()) {
      TcpVideo.getInstance().close();
    }
    //    TcpPcm.getInstance().disconnect();

    //    mHandlerThread.quit();//退出消息循环
    //    AudioRecordManager.getInstance().onDestroy();
    //    AudioTrackManager.getInstance().stopPlay();
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
    } else {
      Log.d(TAG, "start fail");
    }
    //    startRecordVoice();
  }

  //定义Camera的回调方法
  private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
    byte[] mPpsSps = new byte[0];
    long timeStamp =System.currentTimeMillis();
    int frameNum = 0;
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
      synchronized (this) {
//        handler.post(task);
       if(System.currentTimeMillis()-timeStamp>=1000){
         Log.d(TAG,"frame num:"+frameNum);
         timeStamp = System.currentTimeMillis();
         frameNum =0;
       }
        if (data == null) {
          return;
        }
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        byte[] dst = new byte[data.length];
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        if (getDgree(mContext) == 0) {
          dst = CameraFormat.rotateNV21Degree90(data, previewSize.width, previewSize.height);
        } else {
          dst = data;
        }
        try {
          int bufferIndex = mMediaCodec.dequeueInputBuffer(5000000);//等待的时间（ms)
          if (bufferIndex >= 0) {
            inputBuffers[bufferIndex].clear();
            mConvertor.convert(dst, inputBuffers[bufferIndex]);
            mMediaCodec.queueInputBuffer(bufferIndex, 0,
              inputBuffers[bufferIndex].position(),
              System.nanoTime() / 1000, 0);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
              ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
              byte[] outData = new byte[bufferInfo.size];
              outputBuffer.get(outData);
              //记录pps和sps
              if (outData[0] == 0 && outData[1] == 0 && outData[2] == 0 && outData[3] == 1 && outData[4] == 103) {
                mPpsSps = outData;
              } else if (outData[0] == 0 && outData[1] == 0 && outData[2] == 0 && outData[3] == 1 && outData[4] == 101) {
                //在关键帧前面加上pps和sps数据
                byte[] iframeData = new byte[mPpsSps.length + outData.length];
                System.arraycopy(mPpsSps, 0, iframeData, 0, mPpsSps.length);
                System.arraycopy(outData, 0, iframeData, mPpsSps.length, outData.length);
                outData = iframeData;
              }
              byte[] length = intToByteArray(outData.length);

              byte[] testdata = new byte[length.length + outData.length + 2];
              byte[] head = {(byte) 0xff, 0x00};
              System.arraycopy(head, 0, testdata, 0, 2);
              System.arraycopy(length, 0, testdata, 2, length.length);
              System.arraycopy(outData, 0, testdata, length.length + 2, outData.length);
              frameNum++;
              //              FileUtil.save(testdata, 0, testdata.length, path, true);
              TcpVideo.getInstance().sendImage(testdata);
              Log.d(TAG, "onPreviewCallback thread:" + Thread.currentThread().getName() + " data length:" + testdata.length);
              mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
              outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
          } else {
            Log.e("easypusher", "No buffer available !");
          }
        } catch (Exception e) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          String stack = sw.toString();
          Log.e("save_log", stack);
          e.printStackTrace();
        } finally {
          mCamera.addCallbackBuffer(dst);
        }
      }
    }
  };

  public void setIVideoRecoderListener(IVideoRecoderListener iVideoRecoderListener) {
    mIVideoRecoderListener = iVideoRecoderListener;
  }
}
