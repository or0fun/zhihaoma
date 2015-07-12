package com.fang.call;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fang.business.BusinessHelper;
import com.fang.callsms.MainActivity;
import com.fang.callsms.R;
import com.fang.common.controls.CustomDialog;
import com.fang.common.util.BaseUtil;
import com.fang.common.util.StringUtil;
import com.fang.common.util.ViewUtil;
import com.fang.contact.ContactHelper;
import com.fang.logs.LogCode;
import com.fang.logs.LogOperate;
import com.fang.util.MessageWhat;
import com.fang.weixin.WXConstants;
import com.fang.weixin.WXShareHandler;

import java.util.List;
import java.util.Map;

public class CallRecordDialog implements OnClickListener {

    private final String TAG = "CallRecordDialog";
	public static final int MSG_REMOVE = 100;
	protected View mView;
	protected Context mContext;
	
	protected WindowManager mWindowManager = null;
	protected WindowManager.LayoutParams mLayoutParams = null;
	protected boolean mIsShowing = false;
	// 号码
	private String mNumberString;
	// 联系人
	private String mNameString;
    // 号码信息
    private String mInfoString;

	// 通话记录
	private SingleNumberRecordAdapter mAdapter;

	// logo
	protected ImageButton mLogoImageButton;
	// listview
	protected ListView mListView;
	// 发信人
	protected TextView mSenderTextView;
	// 归属地
	protected TextView mInfoTextView;
	// 内容
	protected TextView mMsgBodyTextView;
	// 复制号码
	protected Button mCopyNumberButton;
	// 添加为联系人／ 复制名字
	protected Button mAddOrCopyButton;

	// type logo
	protected ImageView mTypeIcon;
	// 类型说明
	protected TextView mTypeTip;
	// 记录
	protected TextView mRecordsTextView;

	private Dialog mDialog;
	
	boolean mIsMissed = false;

    private ICallRecordDialogListener mCallDialogListener;

    private WXShareHandler mShareHandler;

	protected Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MessageWhat.NET_REQUEST_NUMBER:
				if (null != msg.obj) {
                    mInfoString = (String) msg.obj;
					mInfoTextView.setText(mInfoString);
				}
				break;
			case MessageWhat.CALL_RECORDS:
				if (null != msg.obj) {
					mRecordsTextView.setText((String) msg.obj);
				}
				break;
			case MSG_REMOVE:
				remove();
				break;
			case MessageWhat.FRESH_CALL_RECORD:
				if (null != msg.obj) {
					List<Map<String, Object>> list = (List<Map<String, Object>>) msg.obj;
					mAdapter = new SingleNumberRecordAdapter(mContext, list);
					mListView.setAdapter(mAdapter);
					if (null == list || list.size() == 0) {
						mListView.setVisibility(View.GONE);
					} else {
						mListView.setVisibility(View.VISIBLE);
					}
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	public CallRecordDialog(Context context, String number, String name) {
		init(context, number, name);
	}

	public CallRecordDialog(Context context, String number, String name,
			String type, int icon, boolean isMissed) {
		this(context, number, name);
        mIsMissed = isMissed;
		setContent(number, name, type, icon, null);
	}
	
	public CallRecordDialog(Context context, String number, String name,
			String type, int icon, ICallRecordDialogListener callDialogListener) {
		this(context, number, name);
		setContent(number, name, type, icon, callDialogListener);
	}

	public CallRecordDialog(Context context, String number, String name,
			Bitmap image) {
		this(context, number, name);
		setContent(number, name, image);
	}

	/**
	 * 初始化
	 * 
	 * @param context
	 * @param number
	 * @param name
	 */
	protected void init(Context context, String number, String name) {
		mContext = context;
		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

        mShareHandler = new WXShareHandler(context);
		
		mView = LayoutInflater.from(context).inflate(R.layout.called_dialog,
				null);

		mLogoImageButton = (ImageButton) mView.findViewById(R.id.logo);
		mLogoImageButton.setOnClickListener(this);

		mView.findViewById(R.id.shareBtn).setOnClickListener(this);
		mView.findViewById(R.id.closeBtn).setOnClickListener(this);
        mView.findViewById(R.id.callBtn).setOnClickListener(this);
        mView.findViewById(R.id.smsBtn).setOnClickListener(this);


		mSenderTextView = (TextView) mView.findViewById(R.id.sender);
		mInfoTextView = (TextView) mView.findViewById(R.id.info);
		mMsgBodyTextView = (TextView) mView.findViewById(R.id.body);
		mCopyNumberButton = (Button) mView.findViewById(R.id.copy);
		mCopyNumberButton.setOnClickListener(this);
		mAddOrCopyButton = (Button) mView.findViewById(R.id.add);
		mAddOrCopyButton.setOnClickListener(this);

		mListView = (ListView) mView.findViewById(R.id.recordlist);

		mTypeIcon = (ImageView) mView.findViewById(R.id.icon);
		mTypeTip = (TextView) mView.findViewById(R.id.tip);
		mRecordsTextView = (TextView) mView.findViewById(R.id.record);

	}

	// 填充内容
	public void setContent(String number, String name, Bitmap icon) {
		mNumberString = number;

		mInfoTextView.setText(mContext.getString(R.string.getting_info));

		if (!StringUtil.isEmpty(mNumberString)) {
			BusinessHelper.getNumberInfo(mContext, mNumberString, myHandler);
			CallHelper.getCallRecordsInfo(mContext, mNumberString, myHandler);
			CallHelper.getCallRecordsList(mContext, mNumberString, myHandler);
		}

		mNameString = name;
		if (!TextUtils.isEmpty(mNameString)) {
			mAddOrCopyButton.setText(mContext.getString(R.string.copyName));
		}
		String senderString = "";
		if (null != mNameString && mNameString.length() > 0
				&& !mNameString.equals(mNumberString)) {
			senderString = mNameString + "(" + mNumberString + ")";
		} else {
			senderString = mNumberString;
		}
		mSenderTextView.setText(senderString);
		mTypeTip.setVisibility(View.GONE);
        if (null != icon) {
            mTypeIcon.setImageBitmap(icon);
        }
        mTypeIcon.setLayoutParams(new LinearLayout.LayoutParams(ViewUtil.dip2px(mContext, 40), ViewUtil.dip2px(mContext, 40)));

	}

	// 填充内容
	public void setContent(String number, String name, String type, int icon, ICallRecordDialogListener callDialogListener) {

        mCallDialogListener = callDialogListener;

		mNumberString = number;
		mInfoTextView.setText(mContext.getString(R.string.getting_info));

		// 获取号码信息
		BusinessHelper.getNumberInfo(mContext, mNumberString, myHandler);
		// 获取该号码通话记录
		CallHelper.getCallRecordsInfo(mContext, mNumberString, myHandler);
		// 获取该号码详细通话记录
		CallHelper.getCallRecordsList(mContext, mNumberString, myHandler);

		mNameString = name;
		// 如果是联系人，就显示为复制姓名，否则显示为添加联系人
		if (mNameString.length() > 0) {
			mAddOrCopyButton.setText(mContext.getString(R.string.copyName));
		} else {
			mAddOrCopyButton.setText(mContext.getString(R.string.add));
		}
		String senderString = "";
		if (!StringUtil.isEmpty(mNameString)
				&& !mNameString.equals(mNumberString)) {
			senderString = mNameString + "(" + mNumberString + ")";
		} else {
			senderString = mNumberString;
		}
		mSenderTextView.setText(senderString);
		mTypeTip.setText(type);
		mTypeTip.setShadowLayer(1F, 2F, 2F, Color.WHITE);
		mTypeIcon.setImageResource(icon);
		if (icon == R.drawable.incoming_type) {
			mTypeTip.setTextColor(mContext.getResources().getColor(
					R.color.incoming));
		} else if (icon == R.drawable.missed_type) {
			mTypeTip.setTextColor(mContext.getResources().getColor(
					R.color.missed));
		} else {
			mTypeTip.setTextColor(mContext.getResources().getColor(
					R.color.outgoing));
		}

        if (mIsMissed) {
            mView.findViewById(R.id.closeBtn).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.shareBtn).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.closeBtn).setVisibility(View.GONE);
            mView.findViewById(R.id.shareBtn).setVisibility(View.VISIBLE);
        }
	}

	/**
	 * 显示弹窗
	 */
	public void show() {
		if (mIsMissed) {
			setLayoutParams();
			if (null != mView) {
				if (mIsShowing) {
					BaseUtil.removeView(mWindowManager, mView);
				}
				mIsShowing = true;
				mWindowManager.addView(mView, mLayoutParams);
			}
		}else {
			if (null == mDialog) {
				mDialog = new CustomDialog.Builder(mContext).setContentView(mView)
						.create();
			}
			mIsShowing = true;
			mDialog.show();
		}
	}

	/**
	 * 关闭弹窗
	 */
	public void remove() {
		if (mIsMissed) {
			if (null != mView) {
				mIsShowing = false;
                BaseUtil.removeView(mWindowManager, mView);
			}
		}else {
			if (null != mDialog) {
				mIsShowing = false;
				mDialog.cancel();
			}
		}
        if (null != mCallDialogListener) {
            mCallDialogListener.remove(mInfoString);
        }
	}

	/**
	 * 复制号码
	 */
	protected void copyNumber() {
		BaseUtil.copy(mContext, mNumberString);
	}

	/**
	 * 复制姓名
	 */
	protected void copyName() {
		BaseUtil.copy(mContext, mNameString);
		Toast.makeText(mContext, "姓名" + mNameString + "已复制。",
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * 跳转到回复
	 * 
	 * @param number
	 */
	protected void gotoReply(String number) {
		Uri smsToUri = Uri.parse("smsto:" + number);
		Intent intent = new Intent(android.content.Intent.ACTION_SENDTO,
				smsToUri);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	@Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shareBtn:
                remove();
                share();
                break;
            case R.id.closeBtn:
                remove();
                break;
            case R.id.copy:
                copyNumber();
                break;
            case R.id.add:
                // 复制姓名或者添加为联系人
                if (TextUtils.isEmpty(mNameString)) {
                    remove();
                    ContactHelper.addContact(mContext, mNumberString);
                } else {
                    copyName();
                }
                break;
            case R.id.smsBtn:
                remove();
                gotoReply(mNumberString);
                break;
            case R.id.callBtn:
                remove();
                BaseUtil.gotoCall(mContext, mNumberString);
                break;
            case R.id.logo:
                // 记录日志
                LogOperate.updateLog(mContext, LogCode.ENTER_MAIN_ACTIVITY);

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClass(mContext, MainActivity.class);
                BaseUtil.startActivityNewTask(mContext, intent);
                break;
            default:
                break;
        }
    }

	/**
	 * 是否正在显示
	 * 
	 * @return
	 */
	public boolean isShowing() {
		if (null == mDialog) {
			return false;
		}
		return mDialog.isShowing();
	}	
	
	// 设置LayoutParams
	private void setLayoutParams() {
		if (null == mLayoutParams) {
			mLayoutParams = new WindowManager.LayoutParams();
			mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
			mLayoutParams.gravity = Gravity.LEFT | Gravity.CENTER;
			mLayoutParams.x = 0;
			mLayoutParams.y = 0;
			mLayoutParams.format = PixelFormat.RGBA_8888;
			mLayoutParams.width = LayoutParams.MATCH_PARENT;
			mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
					| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		}
	}

    /**
     * 分享
     */
    private void share() {
        StringBuilder builder = new StringBuilder(mNumberString);
        if (!TextUtils.isEmpty(mNameString)) {
            builder.append("\n");
            builder.append(mNameString);
        }
        if (!TextUtils.isEmpty(mInfoString)) {
            builder.append("\n");
            builder.append(mInfoString);
        }
        mShareHandler.share(builder.toString(), WXConstants.SHARE_WEIXIN | WXConstants.SHARE_TIMELINE);
    }
}
