package radio.serveion.com;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.BroadcastReceiver;

import radio.serveion.com.R;

public class PlayerService extends Service {

	public enum ACTION {
		VOLUME, PLAY_STREAM, PLAY_LIST, MUTE, UPDATE_SONG_INFO, UPDATE_STATUS, SHOW_LOADING, HIDE_LOADING, SHOW_MSG, UPDATE_TRACKLIST
	}

	private final int TRACKLIST_SIZE = 5;
	private ArrayList<String> trackHistoryList = null;
	private myMediaPlayer player = null;
	private final int NOTIFICATION_ID = 204320202;
	private String stream;
	private bReceiver breceiver;
	private String now_playing = null;
	private PhoneStateListener phoneStateListener = null;
	private int volume_level = 25;
	WifiLock wifiLock;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		breceiver = new bReceiver();
		IntentFilter filter = new IntentFilter("smbradiobroadcast");
		registerReceiver(breceiver, filter);
		// Get the telephony manager
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		// Create a new PhoneStateListener
		phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case android.telephony.TelephonyManager.CALL_STATE_RINGING:
				case android.telephony.TelephonyManager.CALL_STATE_OFFHOOK:
					if (player != null)
						if (!player.IsMute())
							player.mute();
					break;
				case android.telephony.TelephonyManager.CALL_STATE_IDLE:
					if (player != null)
						if (player.IsMute())
							player.mute();
					break;
				}
			}
		};

		// Register the listener with the telephony manager
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		trackHistoryList = new ArrayList<String>();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Bundle bundle = null;
		if (intent != null)
			bundle = intent.getExtras();
		if (bundle != null) {
			if ((stream = bundle.getString("stream")) != null) {
				// Toast.makeText(getApplicationContext(), "URL: " +
				// stream.trim(), 8000).show();
				volume_level = bundle.getInt("audio_level", 25);
				PLayStream(stream.trim());
			}
		} else {
			// get from preferences list
			SharedPreferences prefer = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			stream = prefer.getString("stream", "");
			volume_level = prefer.getInt("last_volume_level", 30);
		}
		ShowNotification();
		WifiManager wm = (WifiManager) getApplicationContext()
				.getSystemService(Context.WIFI_SERVICE);
		wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL,
				"rtugaWifiLock");
		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
		super.onStart(intent, startId);
	}

	@Override
	public void onLowMemory() {
		saveLastState();
		super.onLowMemory();
	}

	@Override
	public void onDestroy() {
		Log.w("PlayerService", "Destroying.... " + stream + "|||"
				+ volume_level);
		HideNotification();
		if (player != null) {
			player.stop();
			player.DisconnectProxy();
		}
		SetAndSendBroadcast(ACTION.UPDATE_STATUS.toString(), "status", "PARADO");
		unregisterReceiver(breceiver);
		if (wifiLock != null)
			if (wifiLock.isHeld())
				wifiLock.release();
		saveLastState();
		super.onDestroy();
	}

	private void saveLastState() {
		SharedPreferences.Editor pEditor = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).edit();
		pEditor.putString("stream", stream);
		pEditor.putInt("last_volume_level", volume_level);
		pEditor.commit();
	}

	private void StartMediaPlayer() {
		player = new myMediaPlayer();
		player.setOnNewInfoListener(new myMediaPlayer.OnNewInfoListener() {

			@Override
			public void OnNewInfo(String info) {

				if (info.contains("song#")) {
					if (info.split("#").length > 1)
						now_playing = info.split("#")[1];
					else
						now_playing = "No info available";
					SetAndSendBroadcast(ACTION.UPDATE_SONG_INFO.toString(),
							"song", now_playing);
					AddItemToTrackList(now_playing);
				} else if (info.contains("showDlg#")) {
					SetAndSendBroadcast(ACTION.SHOW_LOADING.toString(), "null",
							"null");
				} else if (info.contains("hideDlg")) {
					SetAndSendBroadcast(ACTION.HIDE_LOADING.toString(), "null",
							"null");
				} else if (info.contains("mute")) {
					if (info.split("#")[1].equals("true")) {
						String value = "NOW MUTING:";
						SetAndSendBroadcast(ACTION.UPDATE_STATUS.toString(),
								"status", value);
					} else if (info.split("#")[1].equals("false")) {
						String value = "NOW PLAYING:";
						SetAndSendBroadcast(ACTION.UPDATE_STATUS.toString(),
								"status", value);
					}
				} else if (info.contains("radioStatus#")) {
					// ShowStreamNotAvailable();
				}
			}
		});
		player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				return false;
			}
		});
	}

	private void AddItemToTrackList(String item) {
		if (trackHistoryList != null) {
			if (trackHistoryList.size() >= TRACKLIST_SIZE) {
				trackHistoryList.remove(0);
			}
			trackHistoryList.add(item);
		}
	}

	private void PLayStream(String stream) {
		if (player == null)
			StartMediaPlayer();
		player.SetVolume(volume_level);
		player.PlayStream(stream);
	}

	private void SetVolume(int value) {
		if (player != null) {
			player.SetVolume(value);
		}
	}

	private void HideNotification() {
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
				.cancel(NOTIFICATION_ID);
	}

	private void ShowNotification() {
		Notification notification = new Notification(R.drawable.ic_launcher,
				getText(R.string.notification_title), System.currentTimeMillis());
		Context context = getApplicationContext();
		CharSequence contentTitle = getText(R.string.notification_title); //;"MBRadio.fm";
		CharSequence contentText = getText(R.string.notification_message); //"Lo mejor de la musica latina...";
		Intent notificationIntent = new Intent(this,
				RadioServeionActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		nm.notify(NOTIFICATION_ID, notification);

	}

	private void SetAndSendBroadcast(String action, String extraName,
			String extraValue) {
		Intent intent = new Intent("mbradiobroadcast");
		intent.putExtra("action", action);
		intent.putExtra(extraName, extraValue);
		sendBroadcast(intent);
	}

	private void SendBroadcastTrackList() {
		Intent intent = new Intent("mbradiobroadcast");
		intent.putExtra("action", ACTION.UPDATE_TRACKLIST.toString());
		intent.putStringArrayListExtra("track_list", trackHistoryList);
		sendBroadcast(intent);
	}

	public class bReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("smbradiobroadcast")) {
				//Log.e("BroadcastReceiver", intent.getStringExtra("action"));
				String action = intent.getStringExtra("action");
				if (action.equals(ACTION.VOLUME.toString())) {
					int value = intent.getIntExtra("value", 0);
					SetVolume(value);
					saveLastState();
				} else if (action.equals(ACTION.PLAY_STREAM.toString())) {
					String stream = intent.getStringExtra("stream");
					PLayStream(stream.trim());
					saveLastState();
				} else if (action.equals(ACTION.MUTE.toString())) {
					if (player != null)
						player.mute();
				} else if (action.equals(ACTION.UPDATE_SONG_INFO.toString())) {

					SetAndSendBroadcast(ACTION.HIDE_LOADING.toString(), "null",
							"null");
					if (player != null) {
						if (player.IsMute())
							SetAndSendBroadcast(
									ACTION.UPDATE_STATUS.toString(), "status",
									"NOW MUTING:");
						else
							SetAndSendBroadcast(
									ACTION.UPDATE_STATUS.toString(), "status",
									"NOW PLAYING:");
					}
					SetAndSendBroadcast(ACTION.UPDATE_SONG_INFO.toString(),
							"song", now_playing);
				} else if (action.equals(ACTION.UPDATE_TRACKLIST.toString())) {
					SendBroadcastTrackList();
				}
			}
		}
	}
}
