package cn.sintoon.facedemo.bd.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.bd.entities.IdentifyUser;
import cn.sintoon.facedemo.bd.listener.IdentifyListener;
import cn.sintoon.facedemo.bd.remote.BDUtil;
import cn.sintoon.facedemo.bd.remote.IdentifyAsync;
import cn.sintoon.facedemo.utils.AppUtil;

public class RegisterBDActivity extends AppCompatActivity implements IdentifyListener {

    private static final int TAKE_REQUEST_CODE = 200;
    private String currentPicturePath;
    private DetectAsync detectAsync;

    private WaitDialog waitDialog;
    private AddUserAsync addFaceAsync;
    private IdentifyAsync identifyAync;

    public static void start(Context context) {
        Intent intent = new Intent(context, RegisterBDActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_bd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("百度人脸注册");
        setSupportActionBar(toolbar);
        waitDialog = new WaitDialog(this);
        /**
         * 1.获取人脸，拍照或者图片
         * 2.进行人脸检测，确定图片中具有人脸
         * 3.人脸注册
         */

        takePicture();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            close();
            return;
        }
        switch (requestCode) {
            case TAKE_REQUEST_CODE:
                if (!TextUtils.isEmpty(currentPicturePath)) {
                    Log.e("onActivityResult", "path->" + currentPicturePath);
                    detectAsync = new DetectAsync();
                    detectAsync.execute(currentPicturePath);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != detectAsync) {
            detectAsync.cancel(true);
        }
        if (null != addFaceAsync) {
            addFaceAsync.cancel(true);
        }
        if (identifyAync != null) {
            identifyAync.cancel(true);
        }
        if (null != waitDialog) {
            if (waitDialog.isShowing()) {
                waitDialog.dismiss();
            }
            waitDialog = null;
        }
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
        if (null != waitDialog) {
            waitDialog.setWaitText(msg);
            waitDialog.show();
        }
    }

    private void closeWait() {
        if (null != waitDialog) {
            waitDialog.dismiss();
        }
    }

    private void addUser(String action) {
        addFaceAsync = new AddUserAsync();
        addFaceAsync.execute(currentPicturePath, action);
    }

    private void addUser() {
        addFaceAsync = new AddUserAsync();
        addFaceAsync.execute(currentPicturePath);
    }

    private void identifyUser() {
        showWait("人脸识别中，请稍后");
        identifyAync = new IdentifyAsync(this);
        identifyAync.execute(currentPicturePath, "5");
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
    public void onError(int code, String msg) {
        //人脸识别失败
        Log.e("identifyError", "code(" + code + ")msg:" + msg);
        addUser();

    }

    @Override
    public void onSucc(int resultNum, List<IdentifyUser> users) {
        if (resultNum == 1) {  //识别出来一个用户
            new AlertDialog.Builder(this)
                    .setTitle("识别到用户")
                    .setMessage("识别到人脸为" + users.get(0).getUid() + "\n追加或者替换该用户下的人脸")
                    .setNegativeButton("替换", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addUser("replace");
                        }
                    })
                    .setNeutralButton("追加", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addUser();
                        }
                    })
                    .setPositiveButton("取消", null)
                    .show();
            //这里可以考虑 询问
        } else if (resultNum > 1) {
            //识别出来多个用户
            StringBuffer buffer = new StringBuffer();
            for (IdentifyUser user : users) {
                buffer.append(user.getUid());
                buffer.append(",");
            }
            buffer.append("无法继续注册");
            new AlertDialog.Builder(this)
                    .setTitle("识别出多个用户")
                    .setMessage(buffer.toString())
                    .setNeutralButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            closeWait();
                            close();
                        }
                    })
                    .show();
        } else {
            addUser();
        }
    }


    class AddUserAsync extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            showWait("人脸注册中，请稍后");
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String path = strings[0];
            String action = "append";
            if (strings.length > 1) {
                action = strings[1];
            }
            HashMap<String, String> options = new HashMap<>();
            options.put("action_type", action);
            AipFace client = BDUtil.getClient();
            JSONObject jsonObject = client.addUser(BDUtil.uid, "根据deviceId生成的用户", Arrays.asList(BDUtil.groupid), path, options);
            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            Log.e("AddUserAsync", "response->\n" + result.toString());
            try {

                if (!result.has("error_code")) {
                    AppUtil.toast("注册成功,即将关闭");
                    closeWait();
                    close();
                } else {
                    int errorCode = result.getInt("error_code");
                    String error_msg = result.getString("error_msg");
                    AppUtil.toast("注册失败：" + error_msg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                closeWait();
                AppUtil.toast("注册失败");

            }
        }
    }

    class DetectAsync extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            showWait("人脸检测中，请稍后");
        }

        @Override
        protected JSONObject doInBackground(String... paths) {
            String path = paths[0];
            AipFace client = BDUtil.getClient();
            HashMap<String, String> options = new HashMap<>();
            options.put("max_face_num", "1");
            JSONObject detect = client.detect(path, options);
            return detect;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            Log.e("onActivityResult", "response:\n" + result.toString());
            try {
                int resultNum = result.getInt("result_num");
                if (resultNum == 1) {
                    identifyUser();
                } else if (resultNum == 0) {
                    closeWait();
                    AppUtil.toast("没有检测到人脸");
                } else if (resultNum > 1) {
                    closeWait();
                    AppUtil.toast("检测到不止一个人脸");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                closeWait();
                AppUtil.toast("检测失败");
                takePicture();
            }
        }
    }


}
