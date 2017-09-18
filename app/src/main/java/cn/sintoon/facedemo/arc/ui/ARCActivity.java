package cn.sintoon.facedemo.arc.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ARCUtil.init();
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.register:
                IdentifyARCActivity.start(this);
                break;
            case R.id.identify:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ARCUtil.destroy();
    }
}
