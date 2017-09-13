package cn.sintoon.facedemo.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.bd.listener.DetectListener;

/**
 * Created by mxc on 2017/9/11.
 * description:
 */

public class AppUtil {

    private static Context context;

    public static void init(Context context){
        AppUtil.context = context.getApplicationContext();
    }

    public static File mkdir(){
        File file = new File(Environment.getExternalStorageDirectory(),"sintoon/facedemo");
        if (!file.exists()){
            file.mkdirs();
        }
        return file;
    }

    private static Toast toast;
    public static void toast(String msg){
        if (toast==null){
            toast = Toast.makeText(context,msg,Toast.LENGTH_SHORT);
        }
        toast.setText(msg);
        toast.show();
    }

    public static String getClientID(){
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        return imei;
    }



    public static void showWait(WaitDialog waitDialog, String msg){
        if (null!=waitDialog){
            waitDialog.setWaitText(msg);
            waitDialog.show();
        }
    }
    public static void closeWait(WaitDialog waitDialog){
        if (null!=waitDialog){
            waitDialog.cancel();
        }
    }

    public static void finishActivity(final AppCompatActivity appCompatActivity){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                appCompatActivity.finish();
            }
        },500);
    }

}
