package cn.sintoon.facedemo.bd.listener;

/**
 * Created by mxc on 2017/9/12.
 * description: 用户认证
 */

public interface VerifyUserListener {
    void onError(int code, String msg);

    void onSucc(boolean result);
}
