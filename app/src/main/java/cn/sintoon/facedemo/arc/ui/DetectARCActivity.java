package cn.sintoon.facedemo.arc.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.arc.utils.ARCUtil;
import cn.sintoon.facedemo.utils.AppUtil;

public class DetectARCActivity extends AppCompatActivity {

    private WaitDialog mWaitDialog;
    private AutoFitTextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private ImageReader mImageReader;
    private Handler mBackgroudHandler;
    private HandlerThread mBackgroudThread;
    private Size mPreviewSize;
    private int mSensorOrientation;
    private String mCameraId = Integer.toString(CameraCharacteristics.LENS_FACING_FRONT);
    private boolean mFlashSupported;
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1080;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT =  1920;

    public static void start(Context context) {
        Intent intent = new Intent(context, DetectARCActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_arc);
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.setZOrderOnTop(true);
        mHolder = mSurfaceView.getHolder();
        mHolder.setFormat(PixelFormat.TRANSLUCENT);

        getSupportActionBar().setTitle("虹软识别-人脸检测");
        mWaitDialog = new WaitDialog(this);
        AppUtil.showWait(mWaitDialog, null);
    }

    @Override
    protected void onResume() {
        startBackground();
        super.onResume();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackground();
        super.onPause();
    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
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
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            AppUtil.toast("摄像头关闭");
            Log.e("onDisconnected", "camera");

            closeCamera();
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
            AppUtil.finishActivity(DetectARCActivity.this);
        }
    };

    private void createCameraPreviewSession() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface(), surface), sessionStateCallback, mBackgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e("onConfigured", "captureSession");
            if (null == mCameraDevice) {
                AppUtil.toast("摄像头配置失败");
                AppUtil.finishActivity(DetectARCActivity.this);
                return;
            }
            try {
                mCaptureSession = cameraCaptureSession;
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,0);
                mCaptureRequest = mCaptureRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mCaptureRequest, captureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e("onConfigureFailed", "capturesession");
            AppUtil.toast("摄像头配置失败");
            AppUtil.finishActivity(DetectARCActivity.this);
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
            AppUtil.finishActivity(DetectARCActivity.this);
        }
    };

    List<AFD_FSDKFace> faces = new ArrayList<>();
    Paint paint = null;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireNextImage();
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
            faces.clear();

            AFD_FSDKEngine detectClient = ARCUtil.getDetectClient();
            AFD_FSDKError error = detectClient.AFD_FSDK_StillImageFaceDetection(
                    rotateData,rotateWidth,rotateHeight, AFD_FSDKEngine.CP_PAF_NV21, faces);
            Canvas canvas = mHolder.lockCanvas();
            //人脸检测没有出错
            if (error.getCode() == 0) {
                //去除上一次的框框
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (faces.size() >= 1) {
                    AppUtil.closeWait(mWaitDialog);
                    AFD_FSDKFace face = faces.get(0).clone();
                    Log.e("onImageAvailable", "face-->" + face.toString()+",角度："+getDegress(face.getDegree()));
                    if (null == paint) {    //框框样式
                        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(3.0f);
                        paint.setStyle(Paint.Style.STROKE);
                    }
                    //这里只为了调试角度查看 将原始检测图片放上去
//                    YuvImage yuvImage = new YuvImage(rotateData,ImageFormat.NV21,rotateWidth,rotateHeight,null);
//                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    yuvImage.compressToJpeg(new Rect(0,0,rotateWidth,rotateHeight),70,outputStream);
//                    byte[] jpegData = outputStream.toByteArray();
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inSampleSize = 1;
//                    Bitmap bmp = BitmapFactory.decodeStream( new ByteArrayInputStream(jpegData), null, options);
//                    if (null!=bmp) {
//                        canvas.drawBitmap(bmp,0,0,null);
//                    }
                    Rect rect = face.getRect();
                    double dx = mTextureView.getWidth() *1.00 ;
                    double dy = mTextureView.getHeight() *1.00;
                    double bx = dx/rotateWidth;
                    double by = dy/rotateHeight;
                    float x = (float) (rect.left *bx);
                    float t = (float) (rect.top * by);
                    float r = (float) (rect.right * bx);
                    float b = (float) (rect.bottom *by);
                    RectF rectF = new RectF(x,t,r,b);
                    canvas.drawRect(rectF, paint);

                }

            } else {
                AppUtil.toast("code->" + error.getCode());
            }
            mHolder.unlockCanvasAndPost(canvas);
            //保存
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

        }
    };

    private String getDegress(int i) {
        String degress = "0";
        switch (i) {
            case AFD_FSDKEngine.AFD_FOC_0:
                degress = "0";
                break;
            case AFD_FSDKEngine.AFD_FOC_30:
                degress = "30";
                break;
            case AFD_FSDKEngine.AFD_FOC_60:
                degress = "60";
                break;
            case AFD_FSDKEngine.AFD_FOC_90:
                degress = "90";
                break;
            case AFD_FSDKEngine.AFD_FOC_120:
                degress = "120";
                break;
            case AFD_FSDKEngine.AFD_FOC_150:
                degress = "150";
                break;
            case AFD_FSDKEngine.AFD_FOC_180:
                degress = "180";
                break;
            case AFD_FSDKEngine.AFD_FOC_210:
                degress = "210";
                break;
            case AFD_FSDKEngine.AFD_FOC_240:
                degress = "240";
                break;
            case AFD_FSDKEngine.AFD_FOC_270:
                degress = "270";
                break;
            case AFD_FSDKEngine.AFD_FOC_300:
                degress = "300";
                break;
            case AFD_FSDKEngine.AFD_FOC_330:
                degress = "330";
                break;
            default:
                degress = "other";
                break;
        }
        return degress;
    }

    private File createCaptureFile() {
        File mkdir = AppUtil.mkdir();
        Calendar calendar = Calendar.getInstance();
        String name = "ARC_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + calendar.get(Calendar.MILLISECOND) + ".jpg";
        File file = new File(mkdir, name);
        return file;
    }


    private void openCamera(int width, int height) {
        //开始打开摄像头
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                AppUtil.toast("拒绝了摄像头权限，无法打开摄像头");
                AppUtil.finishActivity(this);
                return;
            }
            //配置输出设置
            setUpCameraOutputs(width, height);
            CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
            cm.openCamera(mCameraId, stateCallback, mBackgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cm.getCameraIdList();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = cm.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //要前边的
                if (null == facing || facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new CompareSizesByArea());


                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.e("setUpCameraOutputs", "相机方向--》" + mSensorOrientation + "手机方向-》" + displayRotation);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:  //相机方向和手机方向不一致

                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
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
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.YUV_420_888),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                Log.e("setUpCameraOutputs","preview("+mPreviewSize.getWidth()+","+mPreviewSize.getHeight()+")");
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
                mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroudHandler);


                // We fit the aspect ratio of TextureView to the size of preview we picked.
                //获取配置的 方向信息 ，和我们得到的预览尺寸相匹配

                int orientation = getResources().getConfiguration().orientation;

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

//                // Check if the flash is supported. 闪光灯
//                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void startBackground() {
        mBackgroudThread = new HandlerThread("CameraBackground");
        mBackgroudThread.start();
        mBackgroudHandler = new Handler(mBackgroudThread.getLooper());
    }

    private void stopBackground() {
        try {
            if (null != mBackgroudThread) {
                mBackgroudThread.quitSafely();
                mBackgroudThread.join();
                mBackgroudThread = null;
            }
            if (null != mBackgroudHandler) {
                mBackgroudHandler.removeCallbacksAndMessages(null);
                mBackgroudHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {


        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

}
