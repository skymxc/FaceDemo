package cn.sintoon.facedemo.arc.utils;

import android.text.TextUtils;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mxc on 2017/10/10.
 * description: 存储人脸
 */

public class FaceDB {

    private String mDBPath;
    private List<FaceRegist> mRegister;
    private AFR_FSDKEngine mRecognitionEngine;
    private AFR_FSDKVersion m_fsdkVersion;

    class FaceRegist {
        String mName;
        List<AFR_FSDKFace> mFaces;

        public FaceRegist(String mName) {
            this.mName = mName;
            mFaces = new ArrayList<>();
        }
    }

    public FaceDB(String mDBPath) {
        this.mDBPath = mDBPath;
        mRegister = new ArrayList<>();
        m_fsdkVersion = new AFR_FSDKVersion();
        mRecognitionEngine = ARCUtil.getRecognitionEngine();
        mRecognitionEngine.AFR_FSDK_GetVersion(m_fsdkVersion);
        Log.e("FaceDB","version->"+m_fsdkVersion.toString());
    }



    private boolean saveInfo(){
        try {
            FileOutputStream fos = new FileOutputStream(new File(mDBPath,"/face.txt"));
            ExtOutputStream eos = new ExtOutputStream(fos);
            eos.writeString(m_fsdkVersion.toString()+";"+m_fsdkVersion.getFeatureLevel());
            eos.flush();
            eos.close();
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean loadInfo(){
        try {
            FileInputStream fis = new FileInputStream(new File(mDBPath,"/face.txt"));
            ExtInputStream eis = new ExtInputStream(fis);
            String version = eis.readString();

            if (!TextUtils.isEmpty(version)){
                for (String name = eis.readString();name!=null;name = eis.readString()){
                    mRegister.add(new FaceRegist(name));
                }
            }
            eis.close();
            fis.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadFace(){
        if (loadInfo()){
            for (FaceRegist regist:mRegister){
                Log.e("loadFace","name->"+regist.mName);
                try {
                    FileInputStream fis = new FileInputStream(new File(mDBPath,"/"+ regist.mName+".data"));
                    ExtInputStream eis = new ExtInputStream(fis);
                    AFR_FSDKFace afr = null;
                    do {
                        if (afr!=null){
                            regist.mFaces.add(afr);
                        }
                        afr = new AFR_FSDKFace();
                    }while (eis.readBytes(afr.getFeatureData()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }else{
            if (!saveInfo()){
                Log.e("loadFace","fail");
            }
        }
        return false;
    }

    public void addFace(String name,AFR_FSDKFace face){
        try {
            boolean add = true;
            for (FaceRegist regist :mRegister){
                if (regist.mName.equals(name)){
                    regist.mFaces.add(face);
                    add = false;
                }
            }
            if (add){
                FaceRegist regist = new FaceRegist(name);
                regist.mFaces.add(face);
                mRegister.add(regist);
            }

            if (!new File(mDBPath,"/face.txt").exists()){
                if (!saveInfo()){
                    Log.e("addFace","save fail");
                }
            }

            //save name
            FileOutputStream fos = new FileOutputStream(mDBPath+"/face.txt",true);
            ExtOutputStream eos = new ExtOutputStream(fos);
            eos.writeString(name);
            eos.flush();
            eos.close();
            fos.close();

            //save feature
            fos = new FileOutputStream(mDBPath+"/"+name+".data",true);
            eos = new ExtOutputStream(fos);
            eos.writeBytes(face.getFeatureData());
            eos.flush();
            eos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
