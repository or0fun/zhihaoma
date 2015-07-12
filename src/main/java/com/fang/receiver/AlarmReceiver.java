package com.fang.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fang.common.util.BaseUtil;
import com.fang.sms.SMSHelper;
import com.fang.sms.SendSMSInfo;
import com.fang.util.Util;

import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
	
	public static int requestCode = 0;

	public static String INFO = "info";
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (null != intent) {
        	final Object object = intent.getSerializableExtra(INFO);
        	if (null != object) {
    			final SendSMSInfo info = (SendSMSInfo)object;
                BaseUtil.excute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<String> receiverList = info.getmReceiverList();
                                SMSHelper.sendSMS(receiverList, info.getContent(), null, null);
                            }

                        });
                Util.cancelAlarm(context, info.getResultCode());
        		SMSHelper.removeSMSInfo(context, requestCode);
    		}
		}
    }
    
}
