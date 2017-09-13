package cn.sintoon.facedemo.bd.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.bd.remote.BDUtil;

public class BDActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, BDActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("百度人脸识别");
        BDUtil.init();

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register:
                RegisterBDActivity.start(this);
                break;
            case R.id.identify:
                IdentifyBDActivity.start(this);
                break;
            case R.id.verify:
                VerifyUserBDActivity.start(this);
                break;
            case R.id.del:
                DelUserActivity.start(this);
                break;
        }
    }

}
