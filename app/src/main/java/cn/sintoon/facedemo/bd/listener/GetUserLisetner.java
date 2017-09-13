package cn.sintoon.facedemo.bd.listener;

import cn.sintoon.facedemo.bd.entities.BDUser;

/**
 * Created by mxc on 2017/9/12.
 * description: 用户查询
 */

public interface GetUserLisetner {
    void onError(int code, String msg);

    void onSucc(BDUser user);
}
