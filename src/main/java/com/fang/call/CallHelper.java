package com.fang.call;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;

import com.fang.callsms.R;
import com.fang.common.util.BaseUtil;
import com.fang.common.util.DebugLog;
import com.fang.common.util.StringUtil;
import com.fang.database.NumberDatabaseManager;
import com.fang.datatype.ExtraName;
import com.fang.util.MessageWhat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 电话帮助类
 *
 * @author fang
 */
public class CallHelper {

    private static final String TAG = "CallHelper";

    public static final String PARAM_ICON = "icon";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_DATE = "date";
    public static final String PARAM_DURATION = "duration";
    public static final String PARAM_INFO = "info";
    public static final String PARAM_COUNT = "count";

    private static boolean isHasRead = false;

    private static boolean isReading = false;

    protected static List<Map<String, Object>> mAllCallRecords = new ArrayList<Map<String, Object>>();

    protected static List<ICallRecordListener> mCallRecordListeners = new ArrayList<ICallRecordListener>();

    public static final String[] CALL_RECORD_PARAMETERS = new String[]{
            CallLog.Calls._ID, CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION};

    /**
     * 添加监听者
     *
     * @param listener
     */
    public static void registerLisetener(ICallRecordListener listener) {
        mCallRecordListeners.add(listener);
    }

    public static void unregisterListener(ICallRecordListener listener) {
        mCallRecordListeners.remove(listener);
    }

    /**
     * 获取通话记录
     *
     * @return
     */
    public static List<Map<String, Object>> getCallRecords() {
        return mAllCallRecords;
    }

    /**
     * 获取最近的通话时间
     *
     * @param context
     * @param number
     * @return
     */
    public static String getLastRecordDate(final Context context,
                                           final String number) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.DATE}, "number=?",
                new String[]{number}, CallLog.Calls.DATE + " desc limit 1");
        String lastDateString = null;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                long date = Long.parseLong(cursor.getString(cursor
                        .getColumnIndexOrThrow(Calls.DATE)));
                lastDateString = BaseUtil.longDateToStringDate(date);
            }
            cursor.close();
        }
        return lastDateString;
    }

    private static int getCallTimes(Context context, String number, String selection) {
        int t = 0;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.TYPE}, selection,
                new String[]{number}, null);

        t += getCallTimes(cursor);

        if (number.contains(" ")) {
            cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.TYPE}, selection,
                    new String[]{number.replace(" ", "")}, null);

            t += getCallTimes(cursor);
        }

        if (number.contains("+86")) {
            cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.TYPE}, selection,
                    new String[]{number.replace("+86", "")}, null);

            t += getCallTimes(cursor);
        }

        return t;
    }

    /**
     * 获取通话次数
     *
     * @param number
     * @param callType
     */
    public static int getCallTimes(Context context, String number, int callType) {
        String tmpString = String.format(Locale.US, "number=? and type=%d",
                callType);
        return getCallTimes(context, number, tmpString);
    }

    /**
     * 获取通话次数
     *
     * @param context
     * @param number
     */
    public static int getCallTimes(Context context, String number) {
        return getCallTimes(context, number, "number=? ");
    }

    private static int getCallTimes(Cursor cursor) {
        int t = 0;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                do {
                    // 类型
                    int callType = Integer.parseInt(cursor.getString(cursor
                            .getColumnIndex(Calls.TYPE)));
                    if (callType == CallLogType.INCOMING_TYPE || callType == CallLogType.OUTGOING_TYPE
                            || callType == CallLogType.MISSED_TYPE) {
                        t++;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return t;
    }

    /**
     * 获取上次通话信息
     *
     * @param context
     * @param number
     * @param handler
     */
    public static void getLastRecordInfo(final Context context,
                                         final String number, final Handler handler) {
        if (StringUtil.isEmpty(number)) {
            return;
        }
        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                String lastDateString = getLastRecordDate(context, number);

                if (false == StringUtil.isEmpty(lastDateString)) {
                    if (null != handler) {
                        handler.sendMessage(handler.obtainMessage(
                                MessageWhat.MSG_LAST_RECORD_DATE, context.getString(R.string.last_record) + lastDateString));
                    }
                }
            }
        });
    }

    /**
     * 统计通话记录
     *
     * @param context
     * @param number
     * @param handler
     * @return
     */
    public static void getCallRecordsInfo(final Context context,
                                          final String number, final Handler handler) {
        if (StringUtil.isEmpty(number)) {
            return;
        }
        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                StringBuilder result = new StringBuilder();
                int t = getCallTimes(context, number,
                        CallLogType.INCOMING_TYPE);
                result.append(String.format("来电%d次", t));

                t = getCallTimes(context, number, CallLogType.OUTGOING_TYPE);
                result.append(String.format(" 拨出%d次", t));

                t = getCallTimes(context, number, CallLogType.MISSED_TYPE);
                result.append(String.format(" 未接来电%d次", t));
                if (null != handler) {
                    handler.sendMessage(handler.obtainMessage(
                            MessageWhat.CALL_RECORDS, result.toString()));
                }
            }
        });
    }

    private static List<Map<String, Object>> getCallRecordsList(Cursor cursor) {
        List<Map<String, Object>> callRecords = new ArrayList<Map<String, Object>>();
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                do {
                    Map<String, Object> callRecord = new HashMap<String, Object>();
                    // id
                    int id = cursor.getInt(cursor
                            .getColumnIndex(Calls._ID));
                    // 号码
                    String numberString = cursor.getString(cursor
                            .getColumnIndex(Calls.NUMBER));

                    callRecord.put(ExtraName.PARAM_ID, id);

                    // 类型
                    int callType = Integer.parseInt(cursor.getString(cursor
                            .getColumnIndex(Calls.TYPE)));
                    callRecord.put(PARAM_TYPE, callType);
                    //icon
                    callRecord.put(PARAM_ICON, getIcon(callType));

                    callRecord.put(
                            PARAM_DATE,
                            BaseUtil.longDateToStringDate(Long.parseLong(cursor.getString(cursor
                                    .getColumnIndexOrThrow(Calls.DATE)))));
                    long duration = Long.parseLong(cursor.getString(cursor
                            .getColumnIndexOrThrow(Calls.DURATION)));

                    callRecord.put(PARAM_DURATION,
                            BaseUtil.secondsToString(duration));


                    callRecords.add(callRecord);

                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return callRecords;
    }

    /**
     * 获取通话记录
     *
     * @param context
     * @param number
     * @param selection
     * @return
     */
    private static List<Map<String, Object>> getCallRecordsList(final Context context,
                                                                final String number, String selection, String sortOrder) {

        List<Map<String, Object>> callRecords = new ArrayList<Map<String, Object>>();

        callRecords.addAll(getCallRecordsList(context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                CALL_RECORD_PARAMETERS, selection,
                new String[]{number}, sortOrder)));

        if (number.contains(" ")) {
            callRecords.addAll(getCallRecordsList(context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    CALL_RECORD_PARAMETERS, selection,
                    new String[]{number.replace(" ", "")}, sortOrder)));
        }

        if (number.contains("+86")) {
            callRecords.addAll(getCallRecordsList(context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    CALL_RECORD_PARAMETERS, selection,
                    new String[]{number.replace("+86", "")}, sortOrder)));
        }
        return callRecords;
    }

    /**
     * 统计某个号码的通话记录
     *
     * @param context
     * @param number
     * @param handler
     * @return
     */
    public static void getCallRecordsList(final Context context,
                                          final String number, final Handler handler) {
        if (null == context || null == handler || StringUtil.isEmpty(number)) {
            return;
        }
        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                String sortOrder = String.format("%s desc", CallLog.Calls.DATE);
                List<Map<String, Object>> callRecords = getCallRecordsList(context, number, "number=?", sortOrder);

                handler.sendMessage(handler.obtainMessage(
                        MessageWhat.FRESH_CALL_RECORD, callRecords));
            }
        });
    }

    /**
     * 判断是否未接来电
     *
     * @param context
     * @param number
     * @return
     */
    public static boolean isMissedCall(Context context, String number) {
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, CALL_RECORD_PARAMETERS,
                CallLog.Calls.NUMBER + " = ?", new String[]{number},
                CallLog.Calls.DATE + " desc limit 1");
        boolean isMissCall = false;
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                switch (Integer.parseInt(cursor.getString(cursor
                        .getColumnIndex(Calls.TYPE)))) {
                    case CallLogType.MISSED_TYPE:
                        isMissCall = true;
                        break;
                }
            }
            cursor.close();
        }
        return isMissCall;
    }

    /**
     * 删除通话记录
     *
     * @param context
     * @param id
     * @return
     */
    public static int deleteCallRecord(Context context, int id) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.delete(CallLog.Calls.CONTENT_URI, "_id=?",
                new String[]{id + ""});

    }

    /**
     * 判断未接来电
     *
     * @param intent
     * @return
     */
    public static boolean isMissedIncomingCall(Intent intent) {
        if (null == intent) {
            return false;
        }
        String action = intent.getAction();
        String type = intent.getType();
        if (null != action && action.contains("android.intent.action.VIEW")
                && null != type
                && type.contains("vnd.android.cursor.dir/calls")) {
            return true;
        } else if (null != action
                && action.contains("com.android.phone.action.RECENT_CALLS")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取通话记录
     *
     * @param context
     * @return
     */
    public static void getCallRecordsList(final Context context) {
        DebugLog.d(TAG, "getCallRecordsList");
        if (isReading) {
            DebugLog.d(TAG, "getCallRecordsList: isReading is true");
            return;
        }
        if (null == context || isHasRead) {
            for (ICallRecordListener listener : mCallRecordListeners) {
                listener.onResult(true);
            }
            return;
        }

        isReading = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                String sortOrder = String.format("%s desc", CallLog.Calls.DATE);
                Cursor cursor = context.getContentResolver().query(
                        CallLog.Calls.CONTENT_URI, CALL_RECORD_PARAMETERS, null, null,
                        sortOrder);
                List<Map<String, Object>> callRecords = new ArrayList<Map<String, Object>>();

                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        int count = 0;
                        Map<String, Object> callRecord = null;
                        do {
                            // id
                            int id = cursor.getInt(cursor.getColumnIndex(Calls._ID));
                            // 号码
                            String numberString = cursor.getString(cursor
                                    .getColumnIndex(Calls.NUMBER));
                            if (null == callRecord) {
                                callRecord = new HashMap<String, Object>();
                                count++;
                            } else if (numberString
                                    .equals(callRecord.get(ExtraName.PARAM_NUMBER))) {
                                count++;
                                continue;
                            } else {
                                callRecord.put(PARAM_COUNT, count);
                                count = 0;
                                callRecords.add(callRecord);

                                callRecord = new HashMap<String, Object>();
                            }

                            callRecord.put(ExtraName.PARAM_ID, id);
                            //号码
                            callRecord.put(ExtraName.PARAM_NUMBER, numberString);

                            // 联系人姓名
                            String nameString = cursor.getString(cursor
                                    .getColumnIndexOrThrow(Calls.CACHED_NAME));

                            if (StringUtil.isEmpty(nameString)) {
                                callRecord.put(ExtraName.PARAM_NAME, "");
                            } else {
                                callRecord.put(ExtraName.PARAM_NAME, nameString);
                            }
                            // 类型
                            int callType = Integer.parseInt(cursor.getString(cursor
                                    .getColumnIndex(Calls.TYPE)));
                            callRecord.put(PARAM_TYPE, callType);
                            //icon
                            callRecord.put(PARAM_ICON, getIcon(callType));
                            //日期
                            callRecord.put(PARAM_DATE, cursor.getString(cursor
                                    .getColumnIndexOrThrow(Calls.DATE)));
                            long duration = Long.parseLong(cursor.getString(cursor
                                    .getColumnIndexOrThrow(Calls.DURATION)));

                            //时长
                            callRecord.put(PARAM_DURATION,
                                    BaseUtil.secondsToString(duration));
                            //信息
                            callRecord.put(
                                    PARAM_INFO,
                                    NumberDatabaseManager.getInstance(context).query(
                                            numberString));

                        } while (cursor.moveToNext());

                        //次数
                        callRecord.put(PARAM_COUNT, count);
                        callRecords.add(callRecord);

                    }
                    cursor.close();

                    mAllCallRecords = callRecords;

                    for (ICallRecordListener listener : mCallRecordListeners) {
                        listener.onResult(true);
                    }

                    CallHelper.setHasRead(true);
                    isReading = false;
                }
            }
        }).start();
    }

    /**
     * 获取通话类型对中文描述
     *
     * @param context
     * @param callType
     * @return
     */
    public static String getCallTypeString(Context context, int callType) {

        String tip = "";
        switch (callType) {
            case CallLogType.INCOMING_TYPE:
                tip = context.getString(R.string.record_incoming);
                break;
            case CallLogType.OUTGOING_TYPE:
                tip = context.getString(R.string.record_outgoing);
                break;
            case CallLogType.MISSED_TYPE:
                tip = context.getString(R.string.record_missed);
                break;
            case CallLogType.ADD_TYPE:
                tip = context.getString(R.string.record_add);
                break;
            default:
                tip = context.getString(R.string.record_idle);// 应该是挂断.根据我手机类型判断出的
                break;
        }
        return tip;
    }

    /**
     * 获取通话类型对中文描述
     *
     * @param context
     * @param callType
     * @return
     */
    public static int getCallTypeColor(Context context, int callType) {

        int color = context.getResources().getColor(R.color.black);
        switch (callType) {
            case CallLogType.INCOMING_TYPE:
                color = context.getResources().getColor(R.color.incoming);
                break;
            case CallLogType.OUTGOING_TYPE:
                color = context.getResources().getColor(R.color.outgoing);
                break;
            case CallLogType.MISSED_TYPE:
                color = context.getResources().getColor(R.color.missed);
                break;
        }
        return color;
    }

    public static boolean hasRead() {
        return isHasRead;
    }

    public static void setHasRead(boolean isHasRead) {
        DebugLog.d(TAG, "setHasRead: isHasRead = " + isHasRead);
        CallHelper.isHasRead = isHasRead;
    }

    /**
     * 跳转到拨号界面
     *
     * @param context
     */
    public static void gotoCall(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 根据类型获取icon
     *
     * @param callType
     */
    private static int getIcon(int callType) {
        switch (callType) {
            case CallLogType.INCOMING_TYPE:
                return R.drawable.incoming_type;
            case CallLogType.OUTGOING_TYPE:
                return R.drawable.outgoing_type;
            case CallLogType.MISSED_TYPE:
                return R.drawable.missed_type;
            case CallLogType.ADD_TYPE:
                return R.drawable.add_type;
            default:
                return R.drawable.incoming_type;
        }
    }
}
