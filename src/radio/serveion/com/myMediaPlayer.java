package radio.serveion.com;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class myMediaPlayer extends MediaPlayer {
	// ---- Custom Listeners ----//
	/**
	 * Listener raised when there is new song information available. Also used
	 * to communicate when to show notification dialog boxes to user and to
	 * communicate state changes of player.
	 * <ul>
	 * <li>song#new song information.</li>
	 * <li>showDlg#Notification Dialog Message</li>
	 * <li>hideDlg</li>
	 * <li>mute#state (true or false)</li>
	 * </ul>
	 */
	public interface OnNewInfoListener {
		public abstract void OnNewInfo(String info);
	}

	// ----- ENUMERATIONS ----//
	public enum MediaSource {
		Stream, File, None
	}

	// ----- VARIABLES -------//
	boolean initialized = false;
	private int actualVolume = 0;
	private boolean isPaused = false;
	private MediaSource media_source = MediaSource.None;
	private StreamProxy proxy = null;
	//
	private boolean forceProxy = true;

	// On New Info object to set with listener
	private OnNewInfoListener onNewInfoListener = null;

	private boolean is_playing = false;

	private boolean is_mute = false;

	private boolean is_streaming = false;
	// To detect if last state of mute was on...
	private boolean mute_on_start = false;

	private String mediaSourceName = null;

	final String song = "";

	public void setOnNewInfoListener(OnNewInfoListener listener) {
		onNewInfoListener = listener;
	}

	/**
	 * Handler to get stream info
	 */
	final Handler handlerSong = new Handler() {
		public void handleMessage(Message msg) {
			if (onNewInfoListener != null)
				onNewInfoListener.OnNewInfo("song#" + msg.obj.toString());
		}
	};

	/**
	 * Returns the actual volume level set for music stream.
	 * 
	 * @return The value of set audio volume € [0,90]
	 */
	public int getVolume() {
		return actualVolume;
	}

	public boolean IsPlaying() {
		return is_playing;
	}

	public boolean IsPaused() {
		return isPaused;
	}

	/**
	 * Check if media player is in mute mode.
	 * 
	 * @return True if in mute mode, False else.
	 */
	public boolean IsMute() {
		return is_mute;
	}

	/**
	 * Used to check if media player is stream from Internet.
	 * 
	 * @return True if streaming from Internet, false else.
	 */
	public boolean IsStreaming() {
		return is_streaming;
	}

	/**
	 * Sets initial conditions for media player.
	 * 
	 * @param volume_level
	 *            Last or default start volume level
	 * @param muteOnStart
	 *            Last state of mute.
	 */
	public void setStartFlags(int volume_level, boolean muteOnStart) {
		this.actualVolume = volume_level;
		this.mute_on_start = muteOnStart;
	}

	/**
	 * Initializes the media player
	 */
	private void Initialize() {

		this.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e("MediaPlayer Error", "what=" + what);
				if (onNewInfoListener != null)
					onNewInfoListener.OnNewInfo("hideDlg");
				if (!proxy.getServiceAvailable()) {
					if (onNewInfoListener != null)
						onNewInfoListener
								.OnNewInfo("radioStatus#Not available");
				}
				if(what == 701){
					stop();
					PlayStream(mediaSourceName);
				}
				return true;
			}
		});

		this.setOnInfoListener(new OnInfoListener() {

			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				Log.d("MediaPlayer Info", "what=" + what);
				return false;
			}
		});

		this.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				SetVolume(actualVolume);
				if (mute_on_start) {
					is_mute = true;
					setVolume(0.0f, 0.0f);

				}
				if (onNewInfoListener != null)
					onNewInfoListener.OnNewInfo("mute#"
							+ Boolean.toString(is_mute));
				mp.start();
				is_playing = true;
				if (media_source == MediaSource.Stream) {
					is_streaming = true;
				}
				if (onNewInfoListener != null)
					onNewInfoListener.OnNewInfo("hideDlg");

			}
		});

		this.setLooping(false);
		initialized = true;
	}

	/**
	 * Use this to close sockect connection when service is destroyed...
	 */
	public void DisconnectProxy() {
		if (proxy != null)
			if (proxy.IsRunning())
				proxy.stop();
	}

	@Override
	public void stop(){
		try {
			if (!this.isPlaying())
				return;
			if (media_source == MediaSource.Stream) {
				// if (InfoGetter != null)
				// InfoGetter.StopControling();
			}
			super.stop();
			is_playing = false;
			is_streaming = false;

		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean isPlaying() {
		try {
			return super.isPlaying();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public void reset() {
		try {
			super.reset();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setVolume(float leftVolume, float rightVolume) {
		try {
			super.setVolume(leftVolume, rightVolume);
		} catch (IllegalStateException e) {
			// Log.e("ERROR setVolume", e.getMessage());
		}
	}

	/**
	 * Shuts down stream volume if not muted or sets the actual volume level to
	 * stream.
	 */
	public void mute() {
		if (is_mute) {
			float newVolume = (float) Math.pow(Double.parseDouble(Float
					.toString(((float) actualVolume) / 100.0f)), 2.718F);
			;
			if (!is_mute && newVolume == 0.0f)
				is_mute = true;
			else if (is_mute)
				is_mute = false;
			this.setVolume(newVolume, newVolume);
		} else {
			this.setVolume(0.0f, 0.0f);
			is_mute = true;
		}
		if (onNewInfoListener != null)
			onNewInfoListener.OnNewInfo("mute#" + Boolean.toString(is_mute));
	}

	/**
	 * Plays a file
	 * 
	 * @param fName
	 *            the full file path.
	 */
	public void PlayFile(String fName) {
		if (!initialized)
			this.Initialize();
		if (this.isPlaying()) {
			this.stop();
			is_playing = false;
		}
		this.reset();
		try {
			this.setDataSource(fName);
			this.setAudioStreamType(AudioManager.STREAM_MUSIC);
			this.prepare();
			this.start();
			is_playing = true;
			isPaused = false;
			media_source = MediaSource.File;
		} catch (Exception e) {
			Log.e("MediaPlayer Exception", e.getMessage());
		}

	}

	/**
	 * Starts playing an Internet stream in MP3 format.
	 * 
	 * @param url
	 *            The stream Url or IP address
	 * @param port
	 *            The stream port
	 */
	public void PlayStream(String url, int port) {
		String full_url = url + ":" + Integer.toString(port);
		PlayStream(full_url);
	}

	/**
	 * Starts playing an Internet stream in MP3 format.
	 * 
	 * @param url
	 *            The stream URL or IP address including port number (example:
	 *            http://80.125.20.1:9090)
	 */
	public void PlayStream(String url) {

		boolean error = false;

		if (this.is_playing) {
			if (mediaSourceName.equals(url))
				return;
		}

		media_source = MediaSource.Stream;

		if (!initialized)
			this.Initialize();
		this.stop();
		this.reset();

		try {
			mediaSourceName = url;
			if (onNewInfoListener != null)
				onNewInfoListener
						.OnNewInfo("showDlg#Getting stream. Please wait.");
			this.setDataSource(GetPlayUrl(url));
			this.setAudioStreamType(AudioManager.STREAM_MUSIC);
			this.prepareAsync();
			media_source = MediaSource.Stream;
		} catch (IllegalArgumentException e) {
			media_source = MediaSource.None;
			error = true;
			e.printStackTrace();
		} catch (IllegalStateException e) {
			media_source = MediaSource.None;
			error = true;
			e.printStackTrace();
		} catch (IOException e) {
			media_source = MediaSource.None;
			error = true;
			e.printStackTrace();
		} finally {
			if (error) {
				if (onNewInfoListener != null)
					onNewInfoListener.OnNewInfo("hideDlg");

			}

		}

	}

	/**
	 * Selects the proper stream type accordingly to SDK version
	 * 
	 * @param link
	 * @return
	 */
	private String GetPlayUrl(String link) {
		String playUrl = link;
		int sdkVersion = 0;
		try {
			sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException e) {
		}

		if (sdkVersion < 8 || forceProxy) {
			if (proxy == null) {
				proxy = new StreamProxy();
				proxy.setMetaHandler(handlerSong);
				proxy.init();
				proxy.start();
			} 
			String proxyUrl = String.format("http://127.0.0.1:%d/%s",
					proxy.getPort(), link);
			playUrl = proxyUrl;
		}
		return playUrl;
	}

	/**
	 * Sets the MediaPlayer volume
	 * 
	 * @param volume
	 *            € [0, 1] it will increment for 0/100 to 100/100 (%).
	 */
	public void SetVolume(int volume) {
		actualVolume = volume;
		if (this.is_mute)
			return;
		float newVolume = 0.0f;
		// newVolume = (float) Math.log10(Double.parseDouble(Float
		// .toString(((float) volume + 10.0f) / 10.0f)));
		newVolume = (float) Math.pow(
				Double.parseDouble(Float.toString(((float) volume) / 100.0f)),
				2.7180F);
		this.setVolume(newVolume, newVolume);
	}

	@Override
	public void pause() throws IllegalStateException {
		super.pause();
		isPaused = true;
	}

}
