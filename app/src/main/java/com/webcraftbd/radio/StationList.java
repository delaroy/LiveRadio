package com.webcraftbd.radio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class StationList extends BaseActivity implements OnItemClickListener {
	private Handler handler;
	private ListView listView;
	private TextView empty;
	private ProgressBar progressBar;
	private DrawableListAdapter mAdapter;
	private FrameLayout layout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		
		new Thread(new Runnable() {
			@Override
			public void run() {				
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
						setContentView(R.layout.activity_station_list);		
						loadList();
						} catch (Exception e) {
							finish();
							e.printStackTrace();
						}
					}
				});
			}
		}).start();
	}

	public void loadList() {
		listView = (ListView) findViewById(R.id.list);
		progressBar = (ProgressBar) findViewById(R.id.load);
		empty = (TextView) findViewById(R.id.empty);
		layout = (FrameLayout) findViewById(R.id.list_show);
		listView.setOnItemClickListener(this);
		progressBar.setVisibility(View.VISIBLE);
		layout.setVisibility(View.GONE);

		new Thread(new Runnable() {
			@Override
			public void run() {
				mAdapter = new DrawableListAdapter(StationList.this);

				handler.post(new Runnable() {
					@Override
					public void run() {
						listView.setAdapter(mAdapter);
						progressBar.setVisibility(View.GONE);
						layout.setVisibility(View.VISIBLE);
						if (mAdapter.getCount() == 0) {
							listView.setVisibility(View.GONE);
							empty.setVisibility(View.VISIBLE);
						}
					}
				});
			}
		}).start();
	}

	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long l) {
		MainActivity.stationID = position;
		MainActivity.isStationChanged = true;
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		//unbindDrawables(findViewById(R.id.RootView));
		Runtime.getRuntime().gc();
	}

	private void unbindDrawables(View view) {
		try {
			if (view.getBackground() != null) {
				view.getBackground().setCallback(null);
			}
			if (view instanceof ViewGroup) {
				for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
					unbindDrawables(((ViewGroup) view).getChildAt(i));
				}
				((ViewGroup) view).removeAllViews();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
