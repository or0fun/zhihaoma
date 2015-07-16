package com.fang.base;

import android.os.Bundle;

import com.fang.weixin.WXEntryActivity;
import com.fang.weixin.WXShareHandler;

public abstract class WEActivity extends WXEntryActivity {

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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public WXShareHandler getShareHandler() {
        return mShareHandler;
    }
	
}
