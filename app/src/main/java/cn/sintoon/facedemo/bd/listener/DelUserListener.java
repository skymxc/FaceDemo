package cn.sintoon.facedemo.bd.listener;

/**
 * Created by mxc on 2017/9/12.
 * description:
 */

public interface DelUserListener {
    void onError(int code, String msg);

    void onSucc();
}
