package cn.sintoon.facedemo.bd.remote;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cn.sintoon.facedemo.bd.listener.DetectListener;

/**
 * Created by mxc on 2017/9/12.
 * description:
 */

public class DetectAsync extends AsyncTask<String, Void, JSONObject> {

    private DetectListener listener;

    public DetectAsync(DetectListener listener) {
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        String path = strings[0];
        AipFace client = BDUtil.getClient();
        HashMap<String, String> options = new HashMap<>();
        options.put("max_face_num", "1");
        JSONObject detect = client.detect(path, options);
        return detect;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        Log.i("onActivityResult", "response:\n" + result.toString());
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
                listener.onError(errorCode, errorMsg);
                return;
            }
            int resultNum = result.getInt("result_num");
            listener.onSucc(resultNum);
        } catch (JSONException e) {
            e.printStackTrace();
            listener.onError(-1, "json异常");
        }
    }
}
