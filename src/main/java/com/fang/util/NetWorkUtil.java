package com.fang.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.fang.base.RequestUrl;
import com.fang.common.util.DebugLog;
import com.fang.common.util.StringUtil;
import com.fang.security.AESUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NetWorkUtil {
	private static final String TAG = "NetWorkUtil";
	private static NetWorkUtil mInstance = new NetWorkUtil();
	private AESUtil mAesUtil;

	private NetWorkUtil() {
		mAesUtil = new AESUtil();
	}

	public static NetWorkUtil getInstance() {
		return mInstance;
	}

	/**
	 * 网络是否连接
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager
					.getActiveNetworkInfo();
			if (networkInfo != null) {
				return networkInfo.isConnected();
			}
		}
		return false;
	}

    /**
     * wifi连接
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager
                    .getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
            }
        }
        return false;
    }

	/**
	 * 移动网络是否连接
	 * 
	 * @param context
	 * @return
	 */
	public static boolean is3GAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			DebugLog.d(TAG, "newwork is off");
			return false;
		} else {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info == null) {
				DebugLog.d(TAG, "newwork is off");
				return false;
			} else {
				if (info.isAvailable()) {
					DebugLog.d(TAG, "newwork is on");
					return true;
				}
			}
		}
		DebugLog.d(TAG, "newwork is off");
		return false;
	}

	/**
	 * WIFI 是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isWiFiAnable(Context context) {
		WifiManager mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager.isWifiEnabled()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * WIFI 是否开启
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isWiFiActive(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
			DebugLog.d(TAG, "newwork is on");
			return true;
		}
		DebugLog.d(TAG, "newwork is off");
		return false;
	}

	/**
	 * 查询号码
	 */
	public String searchPhone(final Context context, final String number, String uid) {
		if (StringUtil.isEmpty(number)) {
			return "";
		}
		String str = number;
		if (str.startsWith("+86")) {
			str = str.substring(3);
		}
		str = str.replace(" ", "");
		str = str.replace("-", "");

        String infoString = requestAPI("phone", str, uid);
		if (StringUtil.isEmpty(infoString)) {
			return "";
		}
		infoString = infoString.replace(",请谨防受骗。", "");
		infoString = infoString.replace(number, "");
		infoString = infoString.replace("来自", "");
		return infoString.trim();
	}

	/**
	 * 查询快递
	 */
	public String searchExpress(final Context context, final String company, final String number, String uid) {
		if (StringUtil.isEmpty(number) || StringUtil.isEmpty(company)) {
			return "";
		}
		String str = company + number;
        String infoString = requestAPI("express", str, uid);
		if (StringUtil.isEmpty(infoString)) {
			return "";
		}
		return infoString.trim();
	}
	
	/**
	 * 查询天气
	 */
	public String searchWeather(final Context context, final String city, String uid) {
		if (StringUtil.isEmpty(city)) {
			return "";
		}
        String infoString = requestAPI("weather", city, uid);
		if (StringUtil.isEmpty(infoString)) {
			return "";
		}
		return infoString.trim();
	}

    /**
     * 查询天气
     */
    public String searchWeather(int days, final String city, String uid) {
        if (StringUtil.isEmpty(city)) {
            return "";
        }
        String infoString = requestAPI("weather", city, uid, "&d=" + days);
        if (StringUtil.isEmpty(infoString)) {
            return "";
        }
        return infoString.trim();
    }

    /**
     * 查询农历
     */
    public String searchNongli() {

        String infoString = requestAPI("nongli", "", "");
        if (StringUtil.isEmpty(infoString)) {
            return "";
        }
        if (infoString.startsWith("公元")) {
            return "";
        }
        return infoString.trim();
    }

    /**
     * 获取空气质量
     * @param city
     * @return
     */
    public String searchAir(final String city) {
        if (StringUtil.isEmpty(city)) {
            return "";
        }
        String infoString = requestAPI("air", city, "");
        if (StringUtil.isEmpty(infoString)) {
            return "";
        }
        return infoString.trim();
    }

    /**
     * 聊天
     * @param content
     * @return
     */
    public String chat(final String content, String uid) {
        if (StringUtil.isEmpty(content)) {
            return "";
        }
        String infoString = requestAPI("chat", content, uid);
        if (StringUtil.isEmpty(infoString)) {
            return "";
        }
        return infoString.trim();
    }

    private String requestAPI(String t, String w, String uid) {
        return requestAPI(t, w, uid, null);
    }

    private String requestAPI(String t, String w, String uid, String otherParam) {
        String str = mAesUtil.encrypt(w);
        String url = RequestUrl.API_URL + "?t=" + t + "&w=" + str + "&u=" + uid + "&e=1";
        if (!TextUtils.isEmpty(otherParam)) {
            url += otherParam;
        }
        return getHttpRequest(url);
    }

	/**
	 * 发起请求
	 * 
	 * @param url
	 * @return
	 */
	protected String getHttpRequest(String url) {
		String strResult = "";
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				strResult = EntityUtils.toString(httpResponse.getEntity(),
						"UTF-8");
			}
			DebugLog.d(TAG, strResult);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strResult;

	}
}
