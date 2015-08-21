package com.fang.base;

import android.os.Bundle;

import com.fang.common.util.DebugLog;
import com.fang.weixin.WXEntryActivity;
import com.fang.weixin.WXShareHandler;
import com.umeng.analytics.MobclickAgent;

public abstract class WEActivity extends WXEntryActivity {

    private final String TAG = "WEActivity";
    protected WXShareHandler mShareHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if (null == mShareHandler) {
            mShareHandler = new WXShareHandler(mContext);
        }
	}

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            MobclickAgent.onResume(this);
        } catch (Throwable throwable) {
            DebugLog.e(TAG, throwable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            MobclickAgent.onPause(this);
        } catch (Throwable throwable) {
            DebugLog.e(TAG, throwable);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public WXShareHandler getShareHandler() {
        return mShareHandler;
    }
	
}
