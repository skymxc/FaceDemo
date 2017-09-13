package cn.sintoon.facedemo.bd.remote;

import com.baidu.aip.face.AipFace;

import cn.sintoon.facedemo.ConstantKey;
import cn.sintoon.facedemo.utils.AppUtil;

/**
 * Created by mxc on 2017/9/11.
 * description:
 */

public class BDUtil {

    private static AipFace client ;
    public static String uid;
    public static String groupid="group_test";
    public static void init(){
         client = new AipFace(ConstantKey.BD_APP_ID,ConstantKey.BD_API_KEY,ConstantKey.BD_SECRET_KEY);
        uid = "user_"+ AppUtil.getClientID();
    }

    public static AipFace getClient(){
        if (null==client){
            init();
        }
        return client;
    }

}
