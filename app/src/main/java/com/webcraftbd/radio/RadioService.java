/* 
 * Developed By: Mohammad Zakaria Chowdhury
 * Company: Webcraft Bangladesh
 * Email: zakaria.cse@gmail.com
 * Website: http://www.webcraftbd.com
 */

package com.webcraftbd.radio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.spoledge.aacdecoder.MultiPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

public class RadioService extends Service implements OnErrorListener,
		OnCompletionListener, OnPreparedListener, OnInfoListener {
	private static String app_name;
	private static String lastfm_api_key;

	private MediaPlayer mediaPlayer;
	private MultiPlayer multiPlayer; // For AAC

	private String[] station_names;
	private String[] station_urls;
	private String[] station_types;
	private int current_station_id = 0;
	private Bitmap albumCover = null;
	private String album = "";
	private String artist = "";
	private String track = "";
	private String pre_artist = "";
	private String pre_track = "";

	public static final String MODE_CREATED = "CREATED";
	public static final String MODE_DESTROYED = "DESTROYED";
	public static final String MODE_PREPARED = "PREPARED";
	public static final String MODE_STARTED = "STARTED";
	public static final String MODE_CONNECTING = "CONNECTING";
	public static final String MODE_START_PREPARING = "START_PREPARING";
	public static final String MODE_PLAYING = "PLAYING";
	public static final String MODE_PAUSED = "PAUSED";
	public static final String MODE_STOPPED = "STOPPED";
	public static final String MODE_COMPLETED = "COMPLETED";
	public static final String MODE_ERROR = "ERROR";
	public static final String MODE_BUFFERING_START = "BUFFERING_START";
	public static final String MODE_BUFFERING_END = "BUFFERING_END";
	public static final String MODE_METADATA_UPDATED = "METADATA_UPDATED";
	public static final String MODE_ALBUM_UPDATED = "ALBUM_UPDATED";

	private String STATUS_CONNECTING;
	private String STATUS_BUFFERING;
	private String STATUS_READY;
	private String STATUS_PLAYING;
	private String STATUS_PAUSED;
	private String STATUS_STOPPED;
	private String STATUS_ERROR;
	private String STATUS_NOCONNECTION;

	private String status;
	private boolean isPrepared = false;
	private boolean isPreparingStarted = false;
	private boolean isRadioPlaying = false;

	private int timeCounter = -1;
	private String playingTime;

	private final Handler handler = new Handler();
	private final IBinder binder = new RadioBinder();

	private static final String TYPE_AAC = "aac";
	private static final String TYPE_MP3 = "mp3";
	private static final int AAC_BUFFER_CAPACITY_MS = 5000;
	private static final int AAC_DECODER_CAPACITY_MS = 700;

	// Notification
	private static final int NOTIFY_ME_ID = 12345;
	private NotificationCompat.Builder notifyBuilder;
	private NotificationManager notifyMgr = null;

	private RadioMetadata metadata;

	private Timer metadataTimer;
	private Timer playtimeTimer;
	private int[] multiUrlTrack;
	private ArrayList<ArrayList<String>> multiUrl;
	private PlsParser parser;

	private SharedPreferences preferences;

	@Override
	public void onCreate() {
		/* Create MediaPlayer when it starts for first time */

		app_name = getResources().getString(R.string.app_name);
		lastfm_api_key = getResources().getString(R.string.lastfm_api_key);

		station_names = getResources().getStringArray(R.array.station_names);
		station_urls = getResources().getStringArray(R.array.station_urls);
		station_types = getResources().getStringArray(R.array.station_types);

		STATUS_CONNECTING = getResources()
				.getString(R.string.status_connecting);
		STATUS_BUFFERING = getResources().getString(R.string.status_buffering);
		STATUS_READY = getResources().getString(R.string.status_ready);
		STATUS_PLAYING = getResources().getString(R.string.status_playing);
		STATUS_PAUSED = getResources().getString(R.string.status_paused);
		STATUS_STOPPED = getResources().getString(R.string.status_stopped);
		STATUS_ERROR = getResources().getString(R.string.status_error);
		STATUS_NOCONNECTION = getResources().getString(
				R.string.status_noconnection);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		current_station_id = preferences.getInt("current_station_id", 0);

		multiUrl = new ArrayList<ArrayList<String>>();
		multiUrlTrack = new int[station_urls.length];

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < station_urls.length; i++) {
					ArrayList<String> list = new ArrayList<String>();
					multiUrlTrack[i] = 0;
					/*
					 * if (station_urls[i].toLowerCase().contains(".pls") ||
					 * station_urls[i].toLowerCase().contains(".m3u") ||
					 * station_urls[i].toLowerCase().contains(".m3u8") ||
					 * station_urls[i].toLowerCase().contains(".xspf") ||
					 * station_urls[i].toLowerCase().contains(".txt")) { parser
					 * = new PlsParser(station_urls[i]); list =
					 * parser.getUrls(); if (list == null || list.size() == 0) {
					 * list = new ArrayList<String>();
					 * list.add(station_urls[i]); } } else { list = new
					 * ArrayList<String>(); list.add(station_urls[i]); }
					 * station_urls[i] = list.get(0);
					 */
					multiUrl.add(list);
				}
			}
		});

		thread.start();

		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnInfoListener(this);
			multiPlayer = new MultiPlayer(multiPlayerCallback,
					AAC_BUFFER_CAPACITY_MS, AAC_DECODER_CAPACITY_MS);
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		sendBroadcast(new Intent(MODE_CREATED));

		notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// showNotification();

		metadata = new RadioMetadata();

		startRefreshingMetadata();
		startPlayTimer();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		clearServiceData();
		// sendBroadcast(new Intent(MODE_DESTROYED));
	}

	public void clearServiceData() {
		timeCounter = -1;
		stop();
		resetMetadata();
		isPrepared = false;
		exitNotification();
		stopRefreshingMetadata();
		stopPlayTimer();

		current_station_id = preferences.getInt("current_station_id", 0);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// showNotification();
		if (isPlaying())
			setStatus(STATUS_PLAYING);
		else if (isPreparingStarted()) {
			setStatus(STATUS_BUFFERING);
			sendBroadcast(new Intent(MODE_START_PREPARING));
		} else {
			setStatus("");
			sendBroadcast(new Intent(MODE_STARTED));
		}

		/* Starts playback at first time or resumes if it is restarted */
		if (mediaPlayer.isPlaying())
			sendBroadcast(new Intent(MODE_PLAYING));
		else if (isPrepared) {
			sendBroadcast(new Intent(MODE_PAUSED));
		}

		return Service.START_NOT_STICKY;
	}

	@Override
	public void onPrepared(MediaPlayer _mediaPlayer) {
		/* If radio is prepared then start playback */
		setStatus("");
		sendBroadcast(new Intent(MODE_PREPARED));
		isPrepared = true;
		isPreparingStarted = false;
		timeCounter = 0;
		play();
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		/* When no stream found then complete the playback */
		isRadioPlaying = false;
		timeCounter = -1;
		mediaPlayer.stop();
		mediaPlayer.reset();
		resetMetadata();
		isPrepared = false;
		setStatus(STATUS_STOPPED);
		sendBroadcast(new Intent(MODE_COMPLETED));

		setCurrentStationURL2nextSlot();
	}

	public void prepare() {
		/* Prepare Async Task - starts buffering */
		isPreparingStarted = true;

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {

				if (getCurrentStationURL().toLowerCase().contains(".pls")
						|| getCurrentStationURL().toLowerCase()
								.contains(".m3u")
						|| getCurrentStationURL().toLowerCase().contains(
								".m3u8")
						|| getCurrentStationURL().toLowerCase().contains(
								".xspf")
						|| getCurrentStationURL().toLowerCase()
								.contains(".txt")) {

					setStatus(STATUS_CONNECTING);
					sendBroadcast(new Intent(MODE_CONNECTING));
					showNotification();

					parser = new PlsParser(getCurrentStationURL());
					ArrayList<String> list = parser.getUrls();
					if (list == null || list.size() == 0) {
						System.out.println("Playlist: No file found");
						stop();
						return;
					} else {
						station_urls[getCurrentStationID()] = list
								.get(multiUrlTrack[getCurrentStationID()]);
						multiUrl.set(getCurrentStationID(), list);
						System.out.println("Playlist: Selected url - "
								+ station_urls[getCurrentStationID()]);

						// multiUrlTrack[getCurrentStationID()] = 1;
					}
				}

				setStatus(STATUS_BUFFERING);
				sendBroadcast(new Intent(MODE_START_PREPARING));
				showNotification();
				try {
					if (getCurrentStationType().equals(TYPE_AAC))
						multiPlayer.playAsync(getCurrentStationURL());
					else {
						mediaPlayer.setDataSource(getCurrentStationURL());
						mediaPlayer.prepareAsync();
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					stop();
				} catch (NullPointerException e) {
					e.printStackTrace();
					stop();
				} catch (IllegalStateException e) {
					e.printStackTrace();
					stop();
				} catch (IOException e) {
					e.printStackTrace();
					stop();
				} catch (Exception e) {
					e.printStackTrace();
					stop();
				}

			}
		});

		thread.start();
	}

	public boolean isPlaying() {
		if (getCurrentStationType().equals(TYPE_AAC))
			return isRadioPlaying;
		else
			return mediaPlayer.isPlaying();
	}

	public boolean isPreparingStarted() {
		return isPreparingStarted;
	}

	public void play() {
		if (hasConnectivity()) {
			if (getCurrentStationType().equals(TYPE_AAC)) {
				prepare();
			} else {
				if (isPrepared) {
					isRadioPlaying = true;
					mediaPlayer.start();
					System.out.println("RadioService: play");
					setStatus(STATUS_PLAYING);
					sendBroadcast(new Intent(MODE_PLAYING));
				} else {
					prepare();
				}
			}
		} else
			sendBroadcast(new Intent(MODE_STOPPED));

	}

	public void pause() {
		if (!getCurrentStationType().equals(TYPE_AAC)) {
			mediaPlayer.pause();
			isRadioPlaying = false;
			System.out.println("RadioService: pause");
			setStatus(STATUS_PAUSED);
			sendBroadcast(new Intent(MODE_PAUSED));
		}
	}

	public void stop() {

		timeCounter = -1;
		resetMetadata();
		isPrepared = false;
		isPreparingStarted = false;
		System.out.println("RadioService: stop");

		if (getCurrentStationType().equals(TYPE_AAC)) {
			if (isRadioPlaying) {
				isRadioPlaying = false;
				multiPlayer.stop();
			}
			// else
			// sendBroadcast(new Intent(MODE_STOPPED));
		} else {
			mediaPlayer.stop();
			mediaPlayer.reset();

			isRadioPlaying = false;
			setStatus("");
			sendBroadcast(new Intent(MODE_STOPPED));
		}
		// clearNotification();
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		/* Check when buffering is started or ended */
		//
		if (what == 701) { // MediaPlayer.MEDIA_INFO_BUFFERING_START
			isRadioPlaying = false;
			setStatus(STATUS_BUFFERING);
			sendBroadcast(new Intent(MODE_BUFFERING_START));
		} else if (what == 702) { // MediaPlayer.MEDIA_INFO_BUFFERING_END
			isRadioPlaying = true;
			setStatus(STATUS_PLAYING);
			sendBroadcast(new Intent(MODE_BUFFERING_END));
		}

		return false;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		isRadioPlaying = false;
		timeCounter = -1;

		stop();

		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Log.v("ERROR", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK "
					+ extra);
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Log.v("ERROR", "MEDIA ERROR SERVER DIED " + extra);
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Log.v("ERROR", "MEDIA ERROR UNKNOWN " + extra);
			break;
		}

		setStatus(STATUS_ERROR);
		sendBroadcast(new Intent(MODE_ERROR));

		return false;
	}

	public int getCurrentStationID() {
		return current_station_id;
	}

	public void setCurrentStationID(int id) {
		resetMetadata();
		current_station_id = id;
		Editor edit = preferences.edit();
		edit.putInt("current_station_id", id);
		edit.commit();
		updateMetadataURL();
	}

	public String getCurrentStationName() {
		return station_names[current_station_id];
	}

	public String getCurrentStationURL() {
		String url = null;
		try {
			url = station_urls[current_station_id];
		} catch (Exception e) {
			setCurrentStationID(0);
			url = station_urls[current_station_id];
		}
		return url;
	}

	public String getCurrentStationType() {
		if (station_types[current_station_id].toLowerCase().trim()
				.equals("aac")
				|| station_types[current_station_id].toLowerCase().trim()
						.equals("aac+"))
			return TYPE_AAC;
		else
			return TYPE_MP3;
	}

	public void setCurrentStationURL2nextSlot() {
		try {
			if (multiUrlTrack[current_station_id] >= multiUrl.get(
					current_station_id).size()) {
				multiUrlTrack[current_station_id] = 0;
			} else {

				Log.d("PlsParser", "Setting next stream from playlist: "
						+ multiUrlTrack[current_station_id]);

				Log.d("PlsParser",
						"URL = "
								+ multiUrl.get(current_station_id).get(
										multiUrlTrack[current_station_id]));

				station_urls[current_station_id] = multiUrl.get(
						current_station_id).get(
						multiUrlTrack[current_station_id]);
				multiUrlTrack[current_station_id]++;

				play();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getTotalStationNumber() {
		return station_urls.length;
	}

	public Bitmap getAlbumCover() {
		return albumCover;
	}

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public String getTrack() {
		return track;
	}

	public String getPlayingTime() {
		if (timeCounter < 0) {
			playingTime = "";
			return "";
		} else
			return playingTime;
	}

	public void setAlbum(String str) {
		album = str;
	}

	public void setArtist(String str) {
		artist = str;
	}

	public void setTrack(String str) {
		track = str;
	}

	public void setNextStation() {
		resetMetadata();

		if (current_station_id == getTotalStationNumber() - 1)
			setCurrentStationID(0); // current_station_id = 0;
		else
			setCurrentStationID(getCurrentStationID() + 1); // current_station_id
															// += 1;

		updateMetadataURL();
	}

	public void setPreviousStation() {
		resetMetadata();

		if (current_station_id == 0)
			setCurrentStationID(getTotalStationNumber() - 1);// current_station_id
																// =
																// getTotalStationNumber()
																// - 1;
		else
			setCurrentStationID(getCurrentStationID() - 1); // current_station_id
															// -= 1;

		updateMetadataURL();
	}

	public String getStatus() {
		if (!hasConnectivity())
			this.status = STATUS_NOCONNECTION;

		updateNotification(this.status);
		return this.status;
	}

	public void setStatus(String status) {
		if (!hasConnectivity())
			this.status = STATUS_NOCONNECTION;
		else
			this.status = status;

		updateNotification(this.status);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/* Allowing activity to access all methods of RadioService */
	public class RadioBinder extends Binder {
		RadioService getService() {
			return RadioService.this;
		}
	}

	public void showNotification() {
		notifyBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher).setContentTitle(app_name)
				.setContentText("");
		Intent resultIntent = new Intent(this, MainActivity.class);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);

		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		notifyBuilder.setContentIntent(resultPendingIntent);
		notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMgr.notify(NOTIFY_ME_ID, notifyBuilder.build());
	}

	public void clearNotification() {
		if (notifyMgr != null)
			notifyMgr.cancel(NOTIFY_ME_ID);
	}

	public void exitNotification() {
		clearNotification();
		notifyBuilder = null;
		notifyMgr = null;
	}

	public void updateNotification(String status) {
		try {
			if (getTotalStationNumber() > 1) {
				if (status != "")
					status = getCurrentStationName() + " - " + status;
				else
					status = getCurrentStationName();
			}

			if (notifyBuilder != null && notifyMgr != null) {
				notifyBuilder.setContentText(status).setWhen(0);
				notifyMgr.notify(NOTIFY_ME_ID, notifyBuilder.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean hasConnectivity() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	protected class MetadataTask extends AsyncTask<URL, Void, RadioMetadata> {
		private URL stream_url;

		@Override
		protected RadioMetadata doInBackground(URL... urls) {
			try {
				stream_url = urls[0];
				metadata.refreshMeta();
				Log.e("Retrieving MetaData", stream_url.toString());
			} catch (IOException e) {
				// Log.e(MetadataTask.class.toString(), e.getMessage());
				resetMetadata();
			}
			return metadata;
		}

		@Override
		protected void onPostExecute(RadioMetadata result) {
			try {
				// String title_artist=metadata.getStreamTitle();
				String meta_artist = metadata.getArtist();
				String meta_track = metadata.getTitle();

				if (stream_url.toString().equals(getCurrentStationURL())) {
					updateMetadataTitle(meta_artist, meta_track);
				}
				// metaFlag=true;
			} catch (IOException e) {
				Log.e(MetadataTask.class.toString(), e.getMessage());
			}
		}
	}

	public void updateMetadataTitle(String meta_artist, String meta_track) {

		if (meta_track != null) {
			artist = meta_artist;
			track = meta_track;

			if (artist.equals(pre_artist) && track.equals(pre_track)) {
			} else {
				pre_artist = artist;
				pre_track = track;
				clearAlbum();
				sendBroadcast(new Intent(MODE_METADATA_UPDATED));
				System.out.println(artist);
				System.out.println(track);

				updateAlbum();
			}
		} else
			clearAlbum();

	}

	public void startRefreshingMetadata() {
		updateMetadataURL();

		final Handler handler = new Handler();
		metadataTimer = new Timer();
		TimerTask doAsynchronousTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							if (getCurrentStationType().equals(TYPE_MP3)
									&& (isPlaying() || isPreparingStarted)) {
								MetadataTask performBackgroundTask = new MetadataTask();
								performBackgroundTask.execute(new URL(
										getCurrentStationURL()));
							}
						} catch (Exception e) {
							// Log.e(MetadataTask.class.toString(),
							// e.getMessage());
						}
					}
				});
			}
		};
		metadataTimer.schedule(doAsynchronousTask, 0, 10000);
	}

	public void stopRefreshingMetadata() {
		metadataTimer.cancel();
	}

	public void updateMetadataURL() {
		try {
			resetMetadata();
			if (getCurrentStationURL() != null)
				metadata.setStreamUrl(new URL(getCurrentStationURL()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void resetMetadata() {
		artist = "";
		track = "";
		pre_artist = "";
		pre_track = "";
		clearAlbum();
	}

	/* Update Album */
	public void clearAlbum() {
		album = "";
		albumCover = null;
	}

	public void updateAlbum() {
		try {
			String musicInfo[] = { getArtist(), getTrack() };

			if (!musicInfo[0].equals("") && !musicInfo[1].equals(""))
				new LastFMCoverAsyncTask().execute(musicInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class LastFMCoverAsyncTask extends
			AsyncTask<String, Integer, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bmp = null;
			try {
				bmp = LastFMCover.getCoverImageFromTrack(lastfm_api_key,
						params[0], params[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return bmp;
		}

		@Override
		protected void onPostExecute(Bitmap bmp) {
			albumCover = bmp;
			if (bmp != null)
				album = LastFMCover.album;
			else
				album = "";
			sendBroadcast(new Intent(MODE_ALBUM_UPDATED));
			System.out.println(album);
			System.out.println(albumCover);
		}
	}

	/* Play Timer */
	public void startPlayTimer() {
		playtimeTimer = new Timer();
		TimerTask doAsynchronousTask = new TimerTask() {
			@Override
			public void run() {
				if (isRadioPlaying == true) {
					timeCounter++;
					int seconds = timeCounter;
					int minutes = seconds / 60;
					int hours = minutes / 60;
					seconds = seconds % 60;
					minutes = minutes % 60;
					if (hours > 0)
						playingTime = String.format("%02d:%02d:%02d", hours,
								minutes, seconds);
					else
						playingTime = String.format("%02d:%02d", minutes,
								seconds);
				}
			}
		};
		playtimeTimer.schedule(doAsynchronousTask, 0, 1000);
	}

	public void stopPlayTimer() {
		playtimeTimer.cancel();
	}

	/* ----------------- AAC Player ------------------------ */
	public final PlayerCallback multiPlayerCallback = new PlayerCallback() {
		@Override
		public void playerStarted() {
			timeCounter = 0;
			// isRadioPlaying =
			// true;
			// System.out.println("RadioService: play");
			// setStatus(STATUS_PLAYING);
			// sendBroadcast(new
			// Intent(MODE_PLAYING));
		}

		@Override
		public void playerPCMFeedBuffer(boolean isPlaying, int bufSizeMs,
				int bufCapacityMs) {

			float percent = bufSizeMs * 100 / bufCapacityMs;
			System.out.println("Buffer = " + percent + "% , " + bufSizeMs
					+ " / " + bufCapacityMs);

			if (isPlaying == true) {
				isPrepared = true;
				isPreparingStarted = false;

				if (bufSizeMs < 500) {
					isRadioPlaying = false;
					setStatus(STATUS_BUFFERING);
					sendBroadcast(new Intent(MODE_BUFFERING_START));
				} else {
					isRadioPlaying = true;
					setStatus(STATUS_PLAYING);
					sendBroadcast(new Intent(MODE_PLAYING));
				}
			} else {
				isRadioPlaying = false;
				setStatus(STATUS_BUFFERING);
				sendBroadcast(new Intent(MODE_BUFFERING_START));
			}

		}

		@Override
		public void playerStopped(int perf) {

			handler.post(new Runnable() {
				@Override
				public void run() {
					timeCounter = -1;
					playingTime = "";

					isRadioPlaying = false;
					isPreparingStarted = false;
					System.out.println("RadioService: stop");
					setStatus("");
					sendBroadcast(new Intent(MODE_STOPPED));
				}
			});

		}

		@Override
		public void playerException(Throwable t) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					isRadioPlaying = false;
					isPreparingStarted = false;
					playingTime = "";
					timeCounter = -1;
					System.out.println("RadioService: Error Playing Stream");
					setStatus(STATUS_ERROR);
					sendBroadcast(new Intent(MODE_ERROR));
					
					setCurrentStationURL2nextSlot();
				}
			});
		}

		@Override
		public void playerMetadata(String key, String value) {
			System.out.println("Metadata--------------");
			System.out.println(key + " ==> " + value);

			if ("StreamTitle".equals(key) || "icy-name".equals(key)
					|| "icy-description".equals(key)) {
				final String meta_artist = getArtistFromAAC(value);
				final String meta_track = getTrackFromAAC(value);

				handler.post(new Runnable() {
					@Override
					public void run() {
						updateMetadataTitle(meta_artist, meta_track);
					}
				});

			}
		}
	};

	private String getArtistFromAAC(String streamTitle) {
		int end = streamTitle.indexOf("-");
		if (end <= 0)
			end = streamTitle.indexOf(":");

		String title;
		if (end > 0)
			title = streamTitle.substring(0, end);
		else
			title = streamTitle;
		return title.trim();
	}

	private String getTrackFromAAC(String streamTitle) {
		int start = streamTitle.indexOf("-") + 1;
		if (start <= 0)
			start = streamTitle.indexOf(":") + 1;

		String track;
		if (start > 0)
			track = streamTitle.substring(start);
		else
			track = streamTitle;

		int end = streamTitle.indexOf("(");
		if (end > 0)
			track = streamTitle.substring(start, end);

		end = streamTitle.indexOf("[");
		if (end > 0)
			track = streamTitle.substring(start, end);

		return track.trim();
	}

}
