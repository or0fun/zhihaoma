package com.fang.life;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fang.base.BaseFragment;
import com.fang.base.RequestUrl;
import com.fang.callsms.R;
import com.fang.common.CustomConstant;
import com.fang.common.util.BaseUtil;
import com.fang.common.util.DebugLog;
import com.fang.common.util.MapUtil;
import com.fang.common.util.Patterns;
import com.fang.common.util.StringUtil;
import com.fang.datatype.ExtraName;
import com.fang.express.ExpressListActivity;
import com.fang.logs.LogCode;
import com.fang.logs.LogOperate;
import com.fang.map.BDMapListener;
import com.fang.net.ServerUtil;
import com.fang.span.MySpan;
import com.fang.util.MessageWhat;
import com.fang.util.NetWorkUtil;
import com.fang.util.SharedPreferencesHelper;
import com.fang.util.Util;
import com.fang.weather.WeatherHelper;
import com.fang.widget.SearchView;
import com.fang.zxing.activity.CaptureActivity;

public class LifeFragment extends BaseFragment implements OnClickListener {

	private final String TAG = "NumberFragment";
	//显示结果
	TextView mResultTextView;
	//订餐
	LinearLayout mFoodListLayout;
	//订酒店
	LinearLayout mHouseListLayout;
	//快递
	LinearLayout mExpressListLayout;
	//客服
	LinearLayout mServiceListLayout;
	//快递追踪
	Button mSearchExpressBtn;
    // 天气列表
    LinearLayout mWeatherList;
    // 城市
    TextView mWeatherCity;
    //今天日期
    TextView mToday;
    TextView mHistory;
	//缓存号码
	String mNumberString = "";
	//缓存信息
	String mNumberInfoString = "";
	//粘贴板里的数据
	String mPasteNumberString;
    //农历更新时间
    long mLastNongliUpdateTime = 0;
    //天气更新时间
    long mLastWeatherUpdateTime = 0;

    SearchView mSearchView;

    protected Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageWhat.NET_REQUEST_NUMBER:
                    if (null != msg.obj) {
                        mNumberInfoString = (String) msg.obj;
                        mResultTextView.setText(mNumberInfoString);
                    }
                    break;
                case MessageWhat.NET_REQUEST_WEATHER:
                    if (null != msg.obj) {
                        handlerWeather((String) msg.obj);
                    }
                    break;
                case MessageWhat.NET_REQUEST_NONGLI:
                    if (null != msg.obj) {
                        handlerNongli((String) msg.obj);
                    }
                    break;
                case MessageWhat.NET_REQUEST_AIR:
                    if (null != msg.obj) {
                        handlerAir((String) msg.obj);
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        if (isViewCreated()) {
            return mView;
        }

        View rootView = inflater.inflate(R.layout.number_layout, container,
				false);
		mResultTextView = (TextView) rootView.findViewById(R.id.result);
        MySpan.formatTextView(mContext, mResultTextView, mNumberInfoString, false);

        mSearchView = new SearchView(mContext, rootView.findViewById(R.id.search_view));
		
		mFoodListLayout = (LinearLayout) rootView.findViewById(R.id.foodlistBtn);
		mFoodListLayout.setOnClickListener(this);
		mHouseListLayout = (LinearLayout) rootView.findViewById(R.id.houselistBtn);
		mHouseListLayout.setOnClickListener(this);
		mExpressListLayout = (LinearLayout) rootView.findViewById(R.id.expresslistBtn);
		mExpressListLayout.setOnClickListener(this);
		mServiceListLayout = (LinearLayout) rootView.findViewById(R.id.servicelistBtn);
		mServiceListLayout.setOnClickListener(this);
		
		mSearchExpressBtn = (Button) rootView.findViewById(R.id.searchExpressBtn);
		mSearchExpressBtn.setOnClickListener(this);

        mWeatherList = (LinearLayout) rootView.findViewById(R.id.weatherList);
        mWeatherCity = (TextView) rootView.findViewById(R.id.weather_city);

        mToday = (TextView) rootView.findViewById(R.id.today);

        rootView.findViewById(R.id.history).setOnClickListener(this);
        rootView.findViewById(R.id.news).setOnClickListener(this);
        rootView.findViewById(R.id.qiushi).setOnClickListener(this);
        rootView.findViewById(R.id.yisi).setOnClickListener(this);
        rootView.findViewById(R.id.zhihu).setOnClickListener(this);

        rootView.findViewById(R.id.scan).setOnClickListener(this);

        rootView.findViewById(R.id.mylove).setOnClickListener(this);

        // 显示农历和天气
        handlerNongli(SharedPreferencesHelper.getInstance().getString(SharedPreferencesHelper.NONGLI));
        handlerWeather(SharedPreferencesHelper.getInstance().getString(SharedPreferencesHelper.WEATHER));
        handlerAir(SharedPreferencesHelper.getInstance().getString(SharedPreferencesHelper.AIR));

        return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		DebugLog.d(TAG, "onResume");
		String str = BaseUtil.paste(mContext);
		if (str != null && str.matches(Patterns.NUMBER_PATTERN) && !str.equals(mPasteNumberString)) {
			mSearchView.setText(str);
			mPasteNumberString = str;
		}

        searchWeather();

        searchNongli();
	}

	@Override
	public boolean onBackPressed() {

		return super.onBackPressed();
	}


	@Override
	public void onClick(View view) {
        int id = view.getId();
		if (view == mFoodListLayout) {
			mContext.startActivity(new Intent(NumberServiceHelper.ACTION_FOOD));
		} else if (view == mHouseListLayout) {
			mContext.startActivity(new Intent(NumberServiceHelper.ACTION_HOUSE));
		} else if (view == mExpressListLayout) {
			mContext.startActivity(new Intent(NumberServiceHelper.ACTION_EXPRESS));
		} else if (view == mServiceListLayout) {
			mContext.startActivity(new Intent(NumberServiceHelper.ACTION_SERVICE));
		} else if (view == mSearchExpressBtn) {
			mContext.startActivity(new Intent(mContext, ExpressListActivity.class));
		} else if (id == R.id.scan) {//扫一扫
            Intent openCameraIntent = new Intent(mContext,
                    CaptureActivity.class);
            startActivityForResult(openCameraIntent, 0);
            LogOperate.updateLog(mContext, LogCode.SCAN);
        } else if (id == R.id.history) {
            Util.openUrl(RequestUrl.HISTORY_OF_TODAY);
        } else if (id == R.id.news) {
            Util.openUrl(RequestUrl.NEWS_OF_TODAY);
        } else if (id == R.id.qiushi) {
            Util.openUrl(RequestUrl.QIUSHI);
        } else if (id == R.id.yisi) {
            Util.openUrl(RequestUrl.YISI);
        } else if (id == R.id.zhihu) {
            Util.openUrl(RequestUrl.ZHIHU);
        } else if (id == R.id.mylove) {
            mContext.startActivity(new Intent(mContext, ChatActivity.class));
        }
	}

    private void searchWeather() {
        if (SharedPreferencesHelper.getInstance().getString(SharedPreferencesHelper.WEATHER, "").length() > 0) {
            if (System.currentTimeMillis() - mLastWeatherUpdateTime < CustomConstant.ONE_HOUR) {
                DebugLog.d(TAG, "searchWeather: the time is too short");
                return;
            }
        }
        if (null == BDMapListener.getInstance().getBdLocation()) {
            MapUtil.getInstance().getLocationClient().requestLocation();
            return;
        }

        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                // 最多7天
                String weather = NetWorkUtil.getInstance().searchWeather(7,
                        BDMapListener.getInstance().getBdLocation().getCity(),
                        ServerUtil.getInstance(mContext).getUserID());
                if (!StringUtil.isEmpty(weather)) {
                    mLastWeatherUpdateTime = System.currentTimeMillis();
                    myHandler.sendMessage(myHandler.obtainMessage(MessageWhat.NET_REQUEST_WEATHER, weather));
                }
                //获取空气质量
                String air = NetWorkUtil.getInstance().searchAir(
                        BDMapListener.getInstance().getBdLocation().getCity());
                if (!StringUtil.isEmpty(air)) {
                    myHandler.sendMessage(myHandler.obtainMessage(MessageWhat.NET_REQUEST_AIR, air));
                }
            }
        });
    }


    /**
     * 查农历
     */
    private void searchNongli() {
        if (System.currentTimeMillis() - mLastNongliUpdateTime < CustomConstant.QUARTER_HOUR) {
            DebugLog.d(TAG, "searchNongli: the time is too short");
            return;
        }
        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                String nongli = NetWorkUtil.getInstance().searchNongli();
                if (!StringUtil.isEmpty(nongli)) {
                    mLastNongliUpdateTime = System.currentTimeMillis();
                    myHandler.sendMessage(myHandler.obtainMessage(MessageWhat.NET_REQUEST_NONGLI, nongli));
                }
            }
        });
    }

    /**
     * 处理获取到的天气信息
     * @param weather
     */
    private void handlerWeather(String weather) {
        if(!StringUtil.isEmpty(weather)) {
            SharedPreferencesHelper.getInstance().setString(SharedPreferencesHelper.WEATHER, weather);
            int index = weather.indexOf(" ");
            if (index > 0) {
                String city = weather.substring(0, index);
                weather = weather.substring(index + 1);
                mWeatherCity.setText( city + " " +  mContext.getString(R.string.number_weather));
                String[] str = weather.split("\\|");

                mWeatherList.removeAllViews();
                for (int i = 0; i < str.length; i++) {
                    mWeatherList.addView(WeatherHelper.createWeatherItem(mContext, str[i], i));
                }
            }
        }
    }

    /**
     * 处理获取到的空气信息
     * @param air
     */
    private void handlerAir(String air) {
        if(!StringUtil.isEmpty(air)) {
            SharedPreferencesHelper.getInstance().setString(SharedPreferencesHelper.AIR, air);
            mWeatherCity.setText(air);
        }
    }

    /**
     * 处理获取到的农历信息
     * @param nongli
     */
    private void handlerNongli(final String nongli) {
        if (!StringUtil.isEmpty(nongli)) {
            StringBuilder today = new StringBuilder();
            today.append("今天是");
            if (nongli.trim().length() > 0) {
                today.append(nongli.replace("\n", "<br/>"));
                mToday.setText(Html.fromHtml(today.toString()));

                SharedPreferencesHelper.getInstance().setString(SharedPreferencesHelper.NONGLI, nongli);
            }
        }
    }

    /**
     * 外界调用
     * @param scanResult
     */
    public void setResultText(String scanResult) {
        mNumberInfoString = scanResult;
        MySpan.formatTextView(mContext, mResultTextView, scanResult, false);
        if (!TextUtils.isEmpty(scanResult)) {
            if (scanResult.matches(Patterns.URL_PATTERN)) {
                Util.openUrl(scanResult);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(ExtraName.RESULT);
            setResultText(scanResult);

        }
    }
}
