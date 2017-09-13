package cn.sintoon.facedemo;

import android.app.Application;

import cn.sintoon.facedemo.utils.AppUtil;

/**
 * Created by mxc on 2017/9/11.
 * description:
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppUtil.init(this);
    }
}
