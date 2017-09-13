package cn.sintoon.facedemo.bd.listener;

/**
 * Created by mxc on 2017/9/12.
 * description: 人脸检测
 */

public interface DetectListener {
    void onError(int code, String msg);

    void onSucc(int resultNum);
}
