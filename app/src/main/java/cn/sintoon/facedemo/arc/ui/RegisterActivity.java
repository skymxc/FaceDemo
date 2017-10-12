package cn.sintoon.facedemo.arc.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKFace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.arc.utils.ARCUtil;
import cn.sintoon.facedemo.utils.AppUtil;

public class RegisterActivity extends AppCompatActivity {
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1080;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT =  1920;

    AutoFitTextureView mTexture;
    SurfaceView mSurface;
    SurfaceHolder mHolder;
    WaitDialog mWait;

    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    CameraDevice mCameraDevice;
    CameraCaptureSession mCaptureSession;
    CaptureRequest.Builder mCaptureRequestBuilder;
    CaptureRequest mCaptureRequest;
    String mCameraId = Integer.toString(CameraCharacteristics.LENS_FACING_FRONT);

    int mSensorOrientation;
     Size mPreviewSize;
     boolean mFlashSupported;
    ImageReader mImageReader;

    public static void start(Context context){
        Intent intent = new Intent(context,RegisterActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getSupportActionBar().setTitle("虹软识别-注册");
        mTexture = (AutoFitTextureView) findViewById(R.id.texture);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mHolder = mSurface.getHolder();
        mSurface.setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mWait = new WaitDialog(this);
        AppUtil.showWait(mWait, null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackground();
        if (mTexture.isAvailable()) {
            openCamera(mTexture.getWidth(), mTexture.getHeight());
        } else {
            mTexture.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackground();
    }

    private void startBackground() {
        mBackgroundThread = new HandlerThread("background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackground() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mBackgroundThread = null;
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    List<AFD_FSDKFace> mFaces = new ArrayList<>();
    Paint paint = null;
    AFR_FSDKFace afrFace;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            int width = image.getWidth();
            int height = image.getHeight();
            //进行人脸检测 人脸检测必须要 nv21
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] origin = new byte[buffer.remaining()];
            buffer.get(origin);
            byte[] bytes = ARCUtil.getDataFromImage(image,ARCUtil.COLOR_FormatNV21);

            // 前置摄像头 ，转换270°
            byte[] rotateData =  ARCUtil.rotateYUV420Degree270(bytes,width,height);
            int rotateWidth = height;
            int rotateHeight = width;
            image.close();
            mFaces.clear();

            AFD_FSDKEngine detectClient = ARCUtil.getDetectClient();
            AFD_FSDKError error = detectClient.AFD_FSDK_StillImageFaceDetection(
                    rotateData,rotateWidth,rotateHeight, AFD_FSDKEngine.CP_PAF_NV21, mFaces);
            Canvas canvas = mHolder.lockCanvas();
            //人脸检测没有出错
            if (error.getCode() == 0) {
                //去除上一次的框框
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (mFaces.size() >= 1) {
                    AppUtil.closeWait(mWait);
                    AFD_FSDKFace face = mFaces.get(0).clone();
                    if (null == paint) {    //框框样式
                        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(3.0f);
                        paint.setStyle(Paint.Style.STROKE);
                    }
                    Rect rect = face.getRect();
                    double dx = mTexture.getWidth() *1.00 ;
                    double dy = mTexture.getHeight() *1.00;
                    double bx = dx/rotateWidth;
                    double by = dy/rotateHeight;
                    float x = (float) (rect.left *bx);
                    float t = (float) (rect.top * by);
                    float r = (float) (rect.right * bx);
                    float b = (float) (rect.bottom *by);
                    RectF rectF = new RectF(x,t,r,b);
                    canvas.drawRect(rectF, paint);
                    ARCUtil.getRecognitionEngine().AFR_FSDK_ExtractFRFeature(rotateData,rotateWidth,rotateHeight, AFD_FSDKEngine.CP_PAF_NV21,rect,face.getDegree(),afrFace);
                }

            } else {
                AppUtil.toast("code->" + error.getCode());
            }
            mHolder.unlockCanvasAndPost(canvas);
        }
    };
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            closeCamera();
            AppUtil.toast("相机关闭");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
                Log.e("onError","摄像头连接error->"+error);
            closeCamera();
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCaptureSession = session;
            if (null==mCameraDevice){
                AppUtil.toast("相机会话配置失败");
                closeCamera();
                AppUtil.finishActivity(RegisterActivity.this);
                return;
            }
            try {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mCaptureRequest = mCaptureRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mCaptureRequest,captureCallback,  mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            AppUtil.toast("相机会话配置失败");
            closeCamera();
            AppUtil.finishActivity(RegisterActivity.this);
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            AppUtil.closeWait(mWait);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            AppUtil.toast("摄像头预览失败");
            closeCamera();
            AppUtil.finishActivity(RegisterActivity.this);
        }
    };

    private void createCaptureSession() {
        if (null==mCameraDevice) return;
        SurfaceTexture surfaceTexture = mTexture.getSurfaceTexture();
        assert  surfaceTexture!=null;
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),sessionStateCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null!=mCaptureSession){
            mCaptureSession.close();
        }
        if (null!=mCameraDevice){
            mCameraDevice.close();
        }
        if (null!=mImageReader){
            mImageReader.close();
        }
    }

    private void openCamera(int width, int height) {
        int i = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (i != PackageManager.PERMISSION_GRANTED) {
            AppUtil.toast("没有摄像头权限");
            finish();
            return;
        }
        setUpCameraOutputs(width, height);
        CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cm.openCamera(mCameraId,cameraStateCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String[] cameraIdList = cm.getCameraIdList();
            for (String cameraId : cameraIdList) {

                //要前置摄像头
                CameraCharacteristics cameraCharacteristics = cm.getCameraCharacteristics(cameraId);
                Integer integer = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (null == integer || integer == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }

                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (null == map) {
                    continue;
                }

                Size lagestSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new ARCUtil.CompareSizesByArea());

                //手机显示角度
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //相机角度
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                //
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation ==90||mSensorOrientation ==270){
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation ==90 || mSensorOrientation ==270){
                            swappedDimensions = true;
                        }
                        break;
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                //最大预览尺寸 是 屏幕尺寸
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swappedDimensions) {    //角度不一致 交换尺寸
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                mPreviewSize = ARCUtil.chooseOptimalSize(map.getOutputSizes(ImageFormat.YUV_420_888),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, lagestSize);

                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(),mPreviewSize.getHeight(),ImageFormat.YUV_420_888,1);
                mImageReader.setOnImageAvailableListener(onImageAvailableListener,mBackgroundHandler);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                //获取配置的 方向信息 ，和我们得到的预览尺寸相匹配

                int orientation = getResources().getConfiguration().orientation;

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTexture.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTexture.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

//                // Check if the flash is supported. 闪光灯
//                // Check if the flash is supported.
                Boolean available = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void onClick(View view){
        switch (view.getId()){
            case R.id.register:
                if (mFaces.size()==0){
                    AppUtil.toast("没有检测到人脸");
                    return;
                }else if (mFaces.size()>1){
                    AppUtil.toast("检测到多个人脸，一次只能一个");
                    return;
                }
                try {
                    mCaptureSession.stopRepeating();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                toSave();

                break;
        }
    }

    private void toSave() {

        View layout = getLayoutInflater().inflate(R.layout.dialog_register, null);
        final EditText mEditText = (EditText) layout.findViewById(R.id.editview);
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        new AlertDialog.Builder(RegisterActivity.this)
                .setTitle("请输入注册名字")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(layout)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppUtil.showWait(mWait,"注册中，请稍后");
                        ARCUtil.getDB().addFace(mEditText.getText().toString(),afrFace);
                        AppUtil.closeWait(mWait);
                        AppUtil.toast("注册完成");
                        AppUtil.finishActivity(RegisterActivity.this);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {

                            mCaptureSession.setRepeatingRequest(mCaptureRequest,captureCallback,  mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();


    }

}
