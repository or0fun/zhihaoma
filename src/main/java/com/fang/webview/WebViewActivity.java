package com.fang.webview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fang.base.RequestUrl;
import com.fang.base.WEActivity;
import com.fang.callsms.R;
import com.fang.common.controls.CustomWebView;
import com.fang.common.util.BaseUtil;
import com.fang.common.util.StringUtil;
import com.fang.datatype.ExtraName;
import com.fang.logs.LogCode;
import com.fang.logs.LogOperate;
import com.fang.net.NetResuestHelper;
import com.fang.net.ServerUtil;
import com.fang.weixin.WXConstants;
import com.fang.widget.SearchView;


/**
 * Created by benren.fj on 6/11/15.
 */
public class WebViewActivity extends WEActivity implements View.OnClickListener {

    private CustomWebView mWebView;
    private TextView mTitleTV;
    private ProgressBar mProgressBar;
    private ImageView mBack;
    private ImageView mShare;
    private SearchView mSearchView;
    private View mSearchFrame;

    private View myView;
    private WebChromeClient.CustomViewCallback myCallback;
    private String mUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);
        mWebView = (CustomWebView) findViewById(R.id.webview);
        mTitleTV = (TextView) findViewById(R.id.title);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBack = (ImageView) findViewById(R.id.back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mShare = (ImageView) findViewById(R.id.share);
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareHandler.share(mWebView.getUrl(), mWebView.getTitle(), "",
                        BitmapFactory.decodeResource(mContext.getResources(), R.drawable.we108x108), WXConstants.SHARE_ALL);
            }
        });
        initWebView();

        mSearchView = new SearchView(mContext, findViewById(R.id.search_view));
        mSearchFrame = findViewById(R.id.search_frame);
        mSearchFrame.setVisibility(View.GONE);
        mSearchFrame.setOnClickListener(this);

        findViewById(R.id.search_icon).setOnClickListener(this);
        findViewById(R.id.reload_icon).setOnClickListener(this);

        open(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        open(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setSupportZoom(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mWebView.setWebChromeClient(new MyWebChromeClient());
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setDownloadListener(new MyWebViewDownLoadListener());
    }

    private void open(Intent intent) {
        String url = intent.getStringExtra(ExtraName.URL);
        if (StringUtil.isEmpty(url)) {
            return;
        }
        if (RequestUrl.HISTORY_OF_TODAY.equals(url)) {
            LogOperate.updateLog(mContext, LogCode.HISTORY_OF_TODAY);
        }
        mUrl = url;
        mWebView.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (!hiddenVideoView()) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.search_icon) {
            if (mSearchFrame.getVisibility() == View.VISIBLE) {
                BaseUtil.showKeyBoard(mSearchView.getSearchEditView(), false);
                mSearchFrame.setVisibility(View.GONE);
            } else {
                mSearchFrame.setVisibility(View.VISIBLE);
                mSearchView.getSearchEditView().requestFocus();
                BaseUtil.showKeyBoard(mSearchView.getSearchEditView(), true);
            }
        } else if(id == R.id.reload_icon) {
            mWebView.loadUrl(mUrl);
        } else if(id == R.id.share) {
            mShareHandler.share(mWebView.getUrl(), mWebView.getTitle(), "",
                    BitmapFactory.decodeResource(mContext.getResources(), R.drawable.we108x108), WXConstants.SHARE_ALL);
        } else if (id == R.id.search_frame) {
            BaseUtil.showKeyBoard(mSearchView.getSearchEditView(), false);
            mSearchFrame.setVisibility(View.GONE);
        }
     }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("mailto:") || url.startsWith("geo:") ||url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);

        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            handler.proceed();  // 接受所有网站的证书
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mSearchFrame.setVisibility(View.GONE);
            ServerUtil.getInstance(mContext).request(NetResuestHelper.url, url, null);
        }
    }
    private class MyWebChromeClient extends WebChromeClient{

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            hiddenVideoView();
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);

            if (myCallback != null) {
                myCallback.onCustomViewHidden();
                myCallback = null;
                return;
            }

            ViewGroup parent = (ViewGroup) mWebView.getParent();
            parent.removeView(mWebView);
            parent.addView(view);
            myView = view;
            myCallback = callback;

        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if(newProgress < 100){
                mProgressBar.setVisibility(View.VISIBLE);
            }

            if(newProgress == 100){
                mProgressBar.setVisibility(View.GONE);
            }
            mProgressBar.setProgress(newProgress);
            mProgressBar.postInvalidate();
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message,
                                 JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            mTitleTV.setText(title);
        }
    }

    private class MyWebViewDownLoadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                    long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            finish();
        }

    }

    public boolean hiddenVideoView(){
        if (myView != null) {

            if (myCallback != null) {
                myCallback.onCustomViewHidden();
                myCallback = null ;
            }

            ViewGroup parent = (ViewGroup) myView.getParent();
            parent.removeView( myView);
            parent.addView(mWebView);

            myView = null;
            return true;
        }
        return false;
    }
}
