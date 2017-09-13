package cn.sintoon.facedemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by mxc on 2017/7/27.
 * description:
 */

public class WaitDialog extends Dialog {

    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mWaitTv;
    private String mWaitText;

    public WaitDialog(@NonNull Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wait_dialog);
        mProgressBar = (ProgressBar) findViewById(R.id.wait_bar);
        mWaitTv = (TextView) findViewById(R.id.wait_tv);
    }

    public void setWaitText(String text) {
        if (!TextUtils.isEmpty(text)&& null!=mWaitText) {
            mWaitText = text;
            mWaitTv.setText(mWaitText);
        }
    }

    @Override
    public void show() {
        try {
            if (null != mContext && !((Activity) mContext).isFinishing()) {
                super.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void dismiss() {
        try {
            if (null != mContext && !((Activity) mContext).isFinishing()) {
                super.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        try {
            if (null != mContext && !((Activity) mContext).isFinishing()) {
                super.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
