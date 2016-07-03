/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebviewActivity extends BaseActivity{
	
	private WebView webview;
	private String app_name;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_webview);
		
		app_name = getResources().getString(R.string.app_name);
	}
	
	public void loadUrl(String url, final String title) {	
			webview = (WebView) findViewById(R.id.webView1);
			WebSettings webSettings = webview.getSettings();
			webSettings.setJavaScriptEnabled(true);
			webSettings.setDomStorageEnabled(true);
			webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
			webSettings.setAppCacheMaxSize(1024*1024*8);

			final Activity activity = this;
			 webview.setWebChromeClient(new WebChromeClient() {
				 public void onProgressChanged(WebView view, int progress) {
	                 activity.setTitle(app_name+" - Loading...");
	                 activity.setProgress(progress * 100);
	                    if(progress == 100)
	                       activity.setTitle(app_name+" - "+title);
	                 }
			 });
			 webview.setWebViewClient(new WebViewClient() {
			   public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			     Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
			   }
			 });
			 
			 webview.loadUrl(url);		 
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
            case KeyEvent.KEYCODE_BACK:
                if(webview.canGoBack() == true){
                	webview.goBack();
                }else{
                    finish();
                }
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}
