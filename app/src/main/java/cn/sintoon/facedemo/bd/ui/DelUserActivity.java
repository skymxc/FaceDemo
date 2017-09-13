package cn.sintoon.facedemo.bd.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.WaitDialog;
import cn.sintoon.facedemo.bd.listener.DelUserListener;
import cn.sintoon.facedemo.bd.remote.DelUserAsync;
import cn.sintoon.facedemo.utils.AppUtil;

public class DelUserActivity extends AppCompatActivity {

    private WaitDialog mWaitDialog;

    private DelUserAsync delUserAsync;

    public static void start(Context context){
        Intent intent = new Intent(context,DelUserActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_del_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("注销当前用户");
        mWaitDialog  =new WaitDialog(this);
        mWaitDialog.setWaitText("用户删除中，请稍后");
        delUser();
    }

    private void delUser() {
        delUserAsync = new DelUserAsync(delUserListener);
        delUserAsync.execute();
    }

    private DelUserListener delUserListener = new DelUserListener() {
        @Override
        public void onError(int code, String msg) {
            closeWait();
            AppUtil.toast("用户注销异常："+msg);
            close();
        }

        @Override
        public void onSucc() {
            AppUtil.toast("用户注销成功");
            closeWait();
            close();
        }
    };
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
        if (delUserAsync!=null){
            delUserAsync.cancel(true);
        }
    }
}
