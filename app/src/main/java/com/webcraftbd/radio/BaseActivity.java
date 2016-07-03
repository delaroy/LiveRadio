/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

public class BaseActivity extends Activity {

	private Intent bindIntent;
	private RadioService radioService;

	private static boolean isExitMenuClicked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isExitMenuClicked = false;

		// Bind to the service
		bindIntent = new Intent(this, RadioService.class);
		bindService(bindIntent, radioConnection, Context.BIND_AUTO_CREATE);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isExitMenuClicked == true)
			finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);

		if (radioService.getTotalStationNumber() <= 1) {
			menu.findItem(R.id.stations).setVisible(false);
			menu.findItem(R.id.stations).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;

		final String thisClassName = this.getClass().getName();
		final String thisPackageName = this.getPackageName();

		if (item.getItemId() == R.id.radio) {
			if (!thisClassName.equals(thisPackageName + ".MainActivity")) {
				i = new Intent(this, MainActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				return true;
			}

		} else if (item.getItemId() == R.id.stations) {
			if (!thisClassName.equals(thisPackageName + ".StationList")) {
				i = new Intent(this, StationList.class);
				startActivity(i);
				return true;
			}
		} else if (item.getItemId() == R.id.exit) {
			String title = "Exit Radio";
			String message = "Are you sure to exit the app?";
			String buttonYesString = "Yes";
			String buttonNoString = "No";

			isExitMenuClicked = true;

			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle(title);
			ad.setMessage(message);
			ad.setCancelable(true);
			ad.setPositiveButton(buttonYesString,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							if (radioService != null) {
								radioService.exitNotification();
								radioService.stop();
								radioService.stopService(bindIntent);
								isExitMenuClicked = true;
								finish();
							}
						}
					});

			ad.setNegativeButton(buttonNoString, null);

			ad.show();

			return true;

		} else if (item.getItemId() == R.id.about) {
			if (!thisClassName.equals(thisPackageName + ".AboutActivity")) {
				i = new Intent(this, AboutActivity.class);
				startActivity(i);
				return true;
			}

		} else if (item.getItemId() == R.id.facebook) {
			if (!thisClassName.equals(thisPackageName + ".FacebookActivity")) {
				i = new Intent(this, FacebookActivity.class);
				startActivity(i);
				return true;
			}
		} else if (item.getItemId() == R.id.twitter) {
			if (!thisClassName.equals(thisPackageName + ".TwitterActivity")) {
				i = new Intent(this, TwitterActivity.class);
				startActivity(i);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	// Handles the connection between the service and activity
	private final ServiceConnection radioConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			radioService = ((RadioService.RadioBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			radioService = null;
		}
	};

}
