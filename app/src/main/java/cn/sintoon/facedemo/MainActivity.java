package cn.sintoon.facedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import cn.sintoon.facedemo.arc.ui.ARCActivity;
import cn.sintoon.facedemo.arc.ui.IdentifyARCActivity;
import cn.sintoon.facedemo.bd.ui.BDActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};
            int i = checkSelfPermission(permissions[0]);
            int i1 = checkCallingOrSelfPermission(permissions[2]);
            if (i!= PackageManager.PERMISSION_GRANTED
                    ||i1 !=PackageManager.PERMISSION_GRANTED){
                requestPermissions(permissions,200);
            }
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.bd:
                BDActivity.start(this);
                break;
            case R.id.arc:
                ARCActivity.start(this);
                break;
        }
    }
}
