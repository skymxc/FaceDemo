package cn.sintoon.facedemo.bd.listener;

import java.util.List;

import cn.sintoon.facedemo.bd.entities.IdentifyUser;

/**
 * Created by mxc on 2017/9/11.
 * description: 人脸识别
 */

public interface IdentifyListener {
    void onError(int code, String msg);

    void onSucc(int resultNum, List<IdentifyUser> users);
}
