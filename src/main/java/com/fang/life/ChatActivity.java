package com.fang.life;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fang.base.WEActivity;
import com.fang.callsms.R;
import com.fang.common.util.BaseUtil;
import com.fang.common.util.NetWorkUtil;
import com.fang.common.util.StringUtil;
import com.fang.common.util.ViewUtil;
import com.fang.logs.LogCode;
import com.fang.common.util.LogOperate;
import com.fang.common.util.ServerUtil;
import com.fang.span.MySpan;
import com.fang.util.MessageWhat;
import com.fang.util.RequestUtil;

/**
 * 聊天窗口
 * Created by benren.fj on 7/11/15.
 */
public class ChatActivity extends WEActivity {

    Context mContext;
    LinearLayout mContentList;
    Button mSend;
    EditText mEdit;
    ScrollView mScrollview;

    LinearLayout.LayoutParams mToLP;
    LinearLayout.LayoutParams mFromLP;

    final int MSG_SCROLL_BOTTOM = 1;

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MessageWhat.NET_REQUEST_CHAT:
                    if (msg.obj instanceof String) {
                        String m = (String)msg.obj;
                        if (!TextUtils.isEmpty(m)) {
                            addView(createView(mContext,m, false), mFromLP);
                        }
                    }
                    break;
                case MSG_SCROLL_BOTTOM:
                    ViewUtil.scrollToBottom(mScrollview, mContentList);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);

        mContext = this;

        mScrollview = (ScrollView) findViewById(R.id.scrollview);
        mContentList = (LinearLayout) findViewById(R.id.content_list);
        mSend = (Button) findViewById(R.id.sendBtn);
        mEdit = (EditText) findViewById(R.id.chatEdit);
        mEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP ) {
                    send();
                }
                return false;
            }
        });

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        mToLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mToLP.leftMargin = (int) mContext.getResources().getDimension(R.dimen.xmargin);
        mToLP.gravity = Gravity.LEFT;

        mFromLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mFromLP.rightMargin = (int) mContext.getResources().getDimension(R.dimen.xmargin);
        mToLP.gravity = Gravity.RIGHT;

        //欢迎语
        addView(createView(mContext, mContext.getString(R.string.chat_welcome), false), mFromLP);
    }

    public void send() {
        final String content = mEdit.getText().toString().trim();
        if (content.length() == 0) {
            showTip("聊几句呗，内容不要为空哦");
            return;
        }
        if (!NetWorkUtil.isNetworkConnected(mContext)) {
            showTip("网络未连接哦");
            return;
        }

        addView(createView(mContext, mEdit.getText().toString(), true), mToLP);
        mEdit.setText("");
        BaseUtil.excute(new Runnable() {
            @Override
            public void run() {
                String answer = RequestUtil.getInstance().chat(content,
                        ServerUtil.getInstance(mContext).getUserID());
                if (!StringUtil.isEmpty(answer)) {
                    mHandler.sendMessage(mHandler.obtainMessage(MessageWhat.NET_REQUEST_CHAT, answer));
                }
                LogOperate.updateLog(mContext, LogCode.CHAT);
            }
        });
    }

    protected void addView(View view, LinearLayout.LayoutParams lp) {
        mContentList.addView(view, lp);
        mHandler.sendEmptyMessageDelayed(MSG_SCROLL_BOTTOM, 100);
    }

    protected TextView createView(Context context, String content, boolean isSend) {
        TextView tv = new TextView(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        tv.setSingleLine(false);
        tv.setTextColor(mContext.getResources().getColor(R.color.black));
        tv.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tv.setTextIsSelectable(true);
        if (isSend) {
            tv.setBackgroundResource(R.drawable.chatto_bg_normal);
        } else {
            tv.setBackgroundResource(R.drawable.chatfrom_bg_normal);
        }
        MySpan.formatTextView(context, tv, content, false);
        return tv;
    }
}
