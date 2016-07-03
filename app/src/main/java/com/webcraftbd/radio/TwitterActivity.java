/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import android.os.Bundle;

public class TwitterActivity extends WebviewActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		loadUrl("http://mobile.twitter.com/"+getResources().getString(R.string.twitter_id), getResources().getString(R.string.menu_twitter));		 
	}
}
