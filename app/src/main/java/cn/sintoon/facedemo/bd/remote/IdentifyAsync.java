package cn.sintoon.facedemo.bd.remote;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.sintoon.facedemo.bd.entities.IdentifyUser;
import cn.sintoon.facedemo.bd.listener.IdentifyListener;

/**
 * Created by mxc on 2017/9/11.
 * description: 人脸识别
 */

public class IdentifyAsync extends AsyncTask<String, Void, JSONObject> {

    private IdentifyListener listener;

    public IdentifyAsync(IdentifyListener listener) {
        this.listener = listener;
    }


    @Override
    protected JSONObject doInBackground(String... strings) {
        String path = strings[0];
        int topNum = 1;
        if (strings.length > 1) {
            String topNumStr = strings[1];
            topNum = Integer.parseInt(topNumStr);
        }
        if (topNum < 1) {
            topNum = 1;
        }
        if (topNum > 5) {
            topNum = 5;
        }
        HashMap<String, Object> options = new HashMap<>();
        options.put("user_top_num", topNum);
        AipFace client = BDUtil.getClient();
        JSONObject jsonObject = client.identifyUser(Arrays.asList(BDUtil.groupid), path, options);
        return jsonObject;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        Log.i("IdentifyAsync", "result->\n" + result.toString());
        try {
            if (result.has("error_code")) {
                int errorCode = result.getInt("error_code");
                String errorMsg = result.getString("error_msg");
                listener.onError(errorCode, errorMsg);
                return;
            }
            int resultNum = result.getInt("result_num");
            if (resultNum == 0) {
                //没有识别出人脸
                listener.onSucc(resultNum, null);
                return;
            }
            List<IdentifyUser> users = new ArrayList<>();
            Set<String> uidSet = new HashSet<>();
            JSONArray array = result.getJSONArray("result");
            for (int i = 0; i < resultNum; i++) {
                JSONObject resultItem = (JSONObject) array.get(i);
                String uid = resultItem.getString("uid");
                String userInfo = resultItem.getString("user_info");
                JSONArray scoresArray = resultItem.getJSONArray("scores");//结果数组
                int length = scoresArray.length();
                int[] scores = new int[length];
                boolean yes = false;
                for (int k = 0; k < length; k++) {
                    int scoresInt = scoresArray.getInt(k);
                    scores[k] = scoresInt;
                    if (scoresInt >= 80) { //相似度 达到80
                        yes = true;
                    }
                }
                if (yes) {
                    //是否已经匹配到了该用户
                    if (!uidSet.contains(uid)) {
                        uidSet.add(uid);
                        //匹配到了
                        users.add(new IdentifyUser(uid, BDUtil.groupid, userInfo, scores));
                    }
                }
            }
            listener.onSucc(users.size(), users);

        } catch (JSONException e) {
            e.printStackTrace();
            listener.onError(-1, "未能识别出人脸");
        }
    }


}
