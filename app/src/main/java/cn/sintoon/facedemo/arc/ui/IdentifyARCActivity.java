package cn.sintoon.facedemo.arc.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.utils.AppUtil;

public class IdentifyARCActivity extends AppCompatActivity  {

    private WaitDialog mWaitDialog;
    private TextureView mTextureView;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private String cameraID = "1";
    private ImageReader mImageReader;
    private Handler mBackgroudHandler;
    private HandlerThread mBackgroudThread;
    private Size mPreviewSize;

    public static void start(Context context) {
        Intent intent = new Intent(context, IdentifyARCActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_arc);
        mTextureView = (TextureView) findViewById(R.id.surface);
        getSupportActionBar().setTitle("虹软识别");
        mWaitDialog = new WaitDialog(this);
        AppUtil.showWait(mWaitDialog, null);
    }

    @Override
    protected void onResume() {
        Log.e("onResume","identify");
        startBackground();
        super.onResume();
        if (mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        }else{
            mTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        stopBackground();
        super.onPause();
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.e("onOpened", "camera");
            mCameraDevice = cameraDevice;
            try {
                Surface surface = new Surface(mTextureView.getSurfaceTexture());
                mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), sessionStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                AppUtil.toast("摄像头打开失败");
                AppUtil.finishActivity(IdentifyARCActivity.this);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            AppUtil.toast("摄像头关闭");
            Log.e("onDisconnected", "camera");
            if (null != mCaptureSession) {
                mCaptureSession.close();
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

            if (null != mCaptureSession) {
                mCaptureSession.close();
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
            }
            AppUtil.toast("摄像头打开失败");
            Log.e("onError", "camera");
            AppUtil.finishActivity(IdentifyARCActivity.this);
        }
    };


    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e("onConfigured", "captureSession");
            if (null == mCameraDevice) {
                AppUtil.toast("摄像头配置失败");
                AppUtil.finishActivity(IdentifyARCActivity.this);
                return;
            }
            try {
                mCaptureSession = cameraCaptureSession;
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(new Surface(mTextureView.getSurfaceTexture()));
                previewRequestBuilder.addTarget(mImageReader.getSurface());
                CaptureRequest previewRequest = previewRequestBuilder.build();
                int i = mCaptureSession.setRepeatingRequest(previewRequest, captureCallback, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e("onConfigureFailed", "capturesession");
            AppUtil.toast("摄像头配置失败");
            AppUtil.finishActivity(IdentifyARCActivity.this);
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            AppUtil.closeWait(mWaitDialog);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            AppUtil.toast("摄像头预览失败");
            AppUtil.finishActivity(IdentifyARCActivity.this);
        }
    };

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            Log.e("onImageAvailable", "size("+buffer.remaining()+");thread-->" + Thread.currentThread().getName());
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            File file = createCaptureFile();
//            try {
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(bytes);
//                fos.flush();
//                fos.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            image.close();

        }
    };

    private File createCaptureFile() {
        File mkdir = AppUtil.mkdir();
        Calendar calendar = Calendar.getInstance();
        String name = "ARC_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + calendar.get(Calendar.MILLISECOND) + ".jpg";
        File file = new File(mkdir, name);
        return file;
    }


    private void openCamera(int width,int height) {
        //开始打开摄像头
        CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                AppUtil.toast("拒绝了摄像头权限，无法打开摄像头");
                AppUtil.finishActivity(this);
                return;
            }
            mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroudHandler);
            cm.openCamera(cameraID, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void startBackground() {
        mBackgroudThread = new HandlerThread("background");
        mBackgroudThread.start();
        mBackgroudHandler = new Handler(mBackgroudThread.getLooper());
    }

    private void stopBackground() {
        if (null != mBackgroudThread) {
            mBackgroudThread.quitSafely();
            mBackgroudThread = null;
        }
        if (null != mBackgroudHandler) {
            mBackgroudHandler.removeCallbacksAndMessages(null);
            mBackgroudHandler = null;
        }
    }

}
