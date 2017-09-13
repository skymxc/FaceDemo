package cn.sintoon.facedemo.bd.remote;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

import cn.sintoon.facedemo.bd.listener.VerifyUserListener;


/**
 * Created by mxc on 2017/9/12.
 * description:
 */

public class VerifyUserAsync extends AsyncTask<String,Void,JSONObject> {

    private VerifyUserListener listener;

    public VerifyUserAsync(VerifyUserListener listener) {
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        String path = strings[0];
        String uid = BDUtil.uid;
        if (strings.length>1){
            uid = strings[1];
        }
        HashMap<String,Object> options = new HashMap<>();
        AipFace client = BDUtil.getClient();
        JSONObject jsonObject = client.verifyUser(uid, Arrays.asList(BDUtil.groupid), path, options);
        return jsonObject;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        Log.i("VerifyUser","res->\n"+result.toString());
        try {
            if (result.has("error_code")) {
                int errorCode = -1;
                Object error_code = result.get("error_code");
                String errorMsg = result.getString("error_msg");
                if (error_code instanceof String) {
                    String code = result.getString("error_code");
                    switch (code){
                        case "SDK108":
                            errorMsg="不支持的图片格式";
                            errorCode  = 108;
                            break;
                        case "SDK100":
                            errorMsg= "图片大小超限";
                            error_code = 100;
                            break;
                    }
                } else if (error_code instanceof Integer) {
                    errorCode = result.getInt("error_code");
                }
                listener.onError(errorCode, errorMsg);
                return;
            }
            int resultNum = result.getInt("result_num");
            if (resultNum==0){
                //认证失败
                listener.onSucc(false);
                return;
            }
            JSONArray results = result.getJSONArray("result");
            int resultsInt = results.getInt(0);
            boolean resultB= false;
            if (resultsInt>80){
                resultB = true;
            }
            //认证成功
            listener.onSucc(resultB);
        } catch (JSONException e) {
            e.printStackTrace();
            listener.onError(-1,"json异常");
        }
    }
}
