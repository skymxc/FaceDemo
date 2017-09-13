package cn.sintoon.facedemo.bd.remote;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import cn.sintoon.facedemo.bd.listener.DelUserListener;


/**
 * Created by mxc on 2017/9/12.
 * description:
 */

public class DelUserAsync extends AsyncTask<Void,Void,JSONObject> {

    private DelUserListener listener;

    public DelUserAsync(DelUserListener listener) {
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
        AipFace client = BDUtil.getClient();
        JSONObject jsonObject = client.deleteUser(BDUtil.uid, Arrays.asList(BDUtil.groupid));
        return jsonObject;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        Log.i("DelUserAsync","res->\n"+result.toString());
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
                listener.onSucc();

        } catch (JSONException e) {
            e.printStackTrace();
            listener.onError(-1,"json异常");
        }
    }
}
