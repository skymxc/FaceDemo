package cn.sintoon.facedemo.arc.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import cn.sintoon.facedemo.R;
import cn.sintoon.facedemo.arc.utils.ARCUtil;

public class ARCActivity extends AppCompatActivity {

    public static void start(Context context){
        Intent intent = new Intent(context,ARCActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arc);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("虹软");
        ARCUtil.init();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = checkSelfPermission(Manifest.permission.CAMERA);
            int i2 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int i3 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (i!= PackageManager.PERMISSION_GRANTED
                    ||i2!= PackageManager.PERMISSION_GRANTED
                    ||i3!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},200);
            }
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.register:
                RegisterActivity.start(this);
                break;
            case R.id.identify:
                break;
            case R.id.detect:
                DetectARCActivity.start(this);
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ARCUtil.destroy();
    }
}
