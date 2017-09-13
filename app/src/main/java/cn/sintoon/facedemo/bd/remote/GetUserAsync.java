package cn.sintoon.facedemo.bd.remote;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.sintoon.facedemo.bd.entities.BDUser;
import cn.sintoon.facedemo.bd.listener.GetUserLisetner;

/**
 * Created by mxc on 2017/9/12.
 * description: 查询用户信息
 */

public class GetUserAsync extends AsyncTask<String,Void,JSONObject> {

    private GetUserLisetner lisetner;

    public GetUserAsync(GetUserLisetner lisetner) {
        this.lisetner = lisetner;
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        String user = strings[0];
        AipFace client = BDUtil.getClient();
        JSONObject object = client.getUser(user, Arrays.asList(BDUtil.groupid));
        return object;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        Log.i("GetUserAsync","res->\n"+result.toString());
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
                    }
                } else if (error_code instanceof Integer) {
                    errorCode = result.getInt("error_code");
                }
                lisetner.onError(errorCode, errorMsg);
                return;
            }

            JSONArray array = result.getJSONArray("result");
            String uid = array.getJSONObject(0).getString("uid");
            String info = array.getJSONObject(0).getString("user_info");
            List<String> groups = new ArrayList<>();
            BDUser user = new BDUser(uid,info,groups);
            lisetner.onSucc(user);
        } catch (JSONException e) {
            e.printStackTrace();
            lisetner.onError(-1,"json异常");
        }
    }
}
