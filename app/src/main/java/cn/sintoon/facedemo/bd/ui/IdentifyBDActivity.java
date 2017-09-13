package cn.sintoon.facedemo.bd.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.bd.entities.IdentifyUser;
import cn.sintoon.facedemo.bd.listener.DetectListener;
import cn.sintoon.facedemo.bd.listener.IdentifyListener;
import cn.sintoon.facedemo.bd.remote.DetectAsync;
import cn.sintoon.facedemo.bd.remote.IdentifyAsync;
import cn.sintoon.facedemo.utils.AppUtil;

public class IdentifyBDActivity extends AppCompatActivity {


    private static final int REQUEST_CODE_IMAGE_OP = 300;
    WaitDialog mWaitDialog;

    private static final int TAKE_REQUEST_CODE = 200;
    private String currentPicturePath;
    private IdentifyAsync identifyAync;
    private DetectAsync detectAsync;

    public static void start(Context context) {
        Intent intent = new Intent(context, IdentifyBDActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("百度人脸识别");
        mWaitDialog = new WaitDialog(this);
        choosePath();
    }

    private void choosePath() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"拍照获取", "选择图片","取消"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 1:
                                Intent getImageByalbum = new Intent(Intent.ACTION_GET_CONTENT);
                                getImageByalbum.addCategory(Intent.CATEGORY_OPENABLE);
                                getImageByalbum.setType("image/jpeg");
                                startActivityForResult(getImageByalbum, REQUEST_CODE_IMAGE_OP);
                                break;
                            case 0:
                                takePicture();
                                break;
                            case 2:
                                closeWait();
                                close();
                                break;
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_IMAGE_OP:
                Uri uri = data.getData();
                currentPicturePath = getPath(uri);
                detect();
                break;
            case TAKE_REQUEST_CODE:
                detect();
                break;
        }
    }

    private void detect() {
        if (!TextUtils.isEmpty(currentPicturePath)) {
            Log.e("detect", "path-》" + currentPicturePath);
            showWait("人脸检测，请稍后");
            detectAsync = new DetectAsync(detectListener);
            detectAsync.execute(currentPicturePath);
        } else {
            AppUtil.toast("没有检测到数据");
            choosePath();
        }
    }

    private void identify() {
        showWait("人脸识别中，请稍后");
        identifyAync = new IdentifyAsync(identifyListener);
        identifyAync.execute(currentPicturePath, "5");
    }

    private IdentifyListener identifyListener = new IdentifyListener() {
        @Override
        public void onError(int code, String msg) {
            closeWait();
            AppUtil.toast("人脸识别失败：" + msg);
            choosePath();
        }

        @Override
        public void onSucc(int resultNum, List<IdentifyUser> users) {
            if (resultNum == 0) {
                closeWait();
                AppUtil.toast("没有识别出人脸");
                choosePath();
            } else {
                StringBuffer buffer = new StringBuffer();
                for (IdentifyUser user : users) {
                    buffer.append(user.getUid());
                    buffer.append("(");
                    buffer.append(user.getuInfo());
                    buffer.append(")");
                    buffer.append(",");
                }
                String substring = buffer.substring(0, buffer.length() - 1);
                closeWait();
                new AlertDialog.Builder(IdentifyBDActivity.this)
                        .setTitle("识别出的用户有")
                        .setCancelable(false)
                        .setMessage(substring)
                        .setNeutralButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                close();
                            }
                        })
                        .show();
            }
        }
    };
    private DetectListener detectListener = new DetectListener() {
        @Override
        public void onError(int code, String msg) {
            closeWait();
            AppUtil.toast("人脸检测异常" + msg);
            choosePath();
        }

        @Override
        public void onSucc(int resultNum) {
            if (resultNum == 1) {
                //进行人脸识别
                identify();
            } else if (resultNum == 0) {
                AppUtil.toast("没有检测到人脸");
                choosePath();
            } else {
                AppUtil.toast("检测到" + resultNum + "张人脸");
                choosePath();
            }
        }
    };


    /**
     * @param uri
     * @return
     */
    private String getPath(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    return null;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    return null;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(this, contentUri, selection, selectionArgs);
            }
        }
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor actualimagecursor = managedQuery(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        String end = img_path.substring(img_path.length() - 4);
        if (0 != end.compareToIgnoreCase(".jpg") && 0 != end.compareToIgnoreCase(".png")) {
            return null;
        }
        return img_path;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createCaptureFile();
        if (photoFile != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, photoFile.getAbsolutePath());
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            }
            startActivityForResult(takePictureIntent, TAKE_REQUEST_CODE);
        }
    }

    private File createCaptureFile() {
        File mkdir = AppUtil.mkdir();
        Calendar calendar = Calendar.getInstance();
        String name = "BD_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + calendar.get(Calendar.MILLISECOND) + ".jpg";
        File file = new File(mkdir, name);
        currentPicturePath = file.getAbsolutePath();
        return file;
    }

    private void showWait(String msg) {
        if (null != mWaitDialog) {
            mWaitDialog.setWaitText(msg);
            mWaitDialog.show();
        }
    }

    private void closeWait() {
        if (null != mWaitDialog) {
            mWaitDialog.dismiss();
        }
    }

    public void close() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != detectAsync) {
            detectAsync.cancel(true);
        }
        if (null != identifyAync) {
            identifyAync.cancel(true);
        }
        if (mWaitDialog.isShowing()) {
            mWaitDialog.dismiss();
        }
    }
}
