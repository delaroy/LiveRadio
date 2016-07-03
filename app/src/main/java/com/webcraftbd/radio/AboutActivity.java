/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import android.os.Bundle;

public class AboutActivity extends WebviewActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		loadUrl("file:///android_asset/html/about.html", getResources().getString(R.string.menu_about));		 
		//loadUrl("http://www.yoursite.com/about.html");
	}
}
