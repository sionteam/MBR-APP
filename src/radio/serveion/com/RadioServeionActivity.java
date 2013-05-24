package radio.serveion.com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.ads.AdRequest.ErrorCode;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import radio.serveion.com.PlayerService;
import radio.serveion.com.SlideMenuAdapter.SlideMenuItem;
import sixteenbit.com.embededwebbrowser.BrowserView;

public class RadioServeionActivity extends Activity implements OnClickListener,
		OnTouchListener, OnItemClickListener, OnLongClickListener {
	private CustomViewPager mbradioPager;
	private Context cxt;
	private MBradioPagerAdapter pagerAdapter;
	private boolean menu_on = false;
	private int lastX = 0;
	private String direction = null;
	private String RADIO_NAME = "";
	private String RADIO_URL = "";
	private String DEFAULT_URL = "";
	private String RADIO_NAME_AUX = "";
	private int LAST_VOLUME_LEVEL = 0;
	private int SET_VOLUME_LEVEL = 0;
	private mbReceiver receiver;
	private boolean initialized = false;
	private boolean showAdmobs = false;
	private String admobID;
	private String defaultCover = "";
	// The two TextViews that shows artist name and song title in Player Layout
	private TextView artist, title;
	// Pages indexes
	public int browserPageIndex = 0;

	public enum ACTION {
		VOLUME, PLAY_STREAM, PLAY_LIST, MUTE, UPDATE_SONG_INFO, UPDATE_STATUS, SHOW_LOADING, HIDE_LOADING, SHOW_MSG, UPDATE_TRACKLIST
	}

	/** Called when the activity is first created. */
	// @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!haveNetworkConnection()) {
			setContentView(R.layout.splash_layout);
			LoadSplashImage();
			String text = "Your device must be connected to Internet so you can run the application. Please check your Internet connection and restart application";
			((TextView) findViewById(R.id.status_view)).setText(text);
			// Toast.makeText(
			// getApplicationContext(),
			// "Your device must be connected to Internet so you can run the application. Please check your Internet connection.",
			// Toast.LENGTH_LONG).show();
			// finish();
			return;
		}

		setContentView(R.layout.main_layout);
		findViewById(R.id.main_layout).getViewTreeObserver()
				.addOnGlobalLayoutListener(
						new ViewTreeObserver.OnGlobalLayoutListener() {

							@Override
							public void onGlobalLayout() {
								View v = findViewById(R.id.main_layout);
								// Log.e("Main Layout changed... why?",
								// "left=" + v.getLeft() + " right="
								// + v.getRight());
								if (menu_on && v.getLeft() == 0) {
									int right = findViewById(R.id.side_menu)
											.getLeft();
									int left = right - v.getWidth();
									v.layout(left, v.getTop(), right,
											v.getBottom());
								}
							}
						});
		// LoadLastValues();
		// String extdir = Environment.getExternalStorageState();
		// // Log.i("External Storage", extdir);
		// File extfile = getExternalFilesDir(null);
		// // Log.i("External Storage", extfile.getPath());
		// // Intent intent0 = new Intent(Intent.ACTION_MAIN);
		// // intent0.addCategory(Intent.CATEGORY_HOME);
		// ResolveInfo resolveInfo = getPackageManager().resolveActivity(
		// getIntent(), PackageManager.MATCH_DEFAULT_ONLY);
		// String currentHomePackage = resolveInfo.activityInfo.packageName;
		// Log.i("Package Name", currentHomePackage);

		Intent intent = new Intent(this, SplashActivity.class);

		// intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		startActivityForResult(intent, 121);

	}

	private void LoadSplashImage() {
		SharedPreferences sp = getSharedPreferences("mypreferences",
				MODE_PRIVATE);
		String pathName = sp.getString("splash_image", null);
		if (pathName == null)
			return;
		// Log.e("Splash ...", "Slpash background: " + pathName);
		View v = findViewById(R.id.splash_layout);
		Drawable d = Drawable.createFromPath(pathName);
		v.setBackgroundDrawable(d);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		// super.onActivityResult(requestCode, resultCode, data);
		// Log.w("Serveion Radio", "codes received: " + requestCode + "; "
		// + resultCode + " [" + RESULT_OK + "]");
		if (requestCode == 121) {
			if (resultCode == RESULT_OK)
				InitActivity();
			else if (resultCode == RESULT_CANCELED)
				finish();
		}

	}

	@Override
	public void onContentChanged() {
		// TODO Auto-generated method stub
		super.onContentChanged();
		// Log.e("Serveion Radio Activity", "Content Changed...");
	}

	private void LoadBackgroundImage() {
		SharedPreferences sp = getSharedPreferences("mypreferences",
				MODE_PRIVATE);
		String pathName = sp.getString("bkg_image", null);
		if (pathName == null)
			return;
		// Log.e("Splash ...", "Background: " + pathName);
		View v = findViewById(R.id.main_layout);
		Drawable d = Drawable.createFromPath(pathName);
		v.setBackgroundDrawable(d);
	}

	public void InitActivity() {
		LoadBackgroundImage();
		// Log.w("Serveion Radio Activity", "Initializing...");
		LoadLastValues();

		initialized = true;
		if (RADIO_NAME == null)
			RADIO_NAME = RADIO_NAME_AUX;
		SlideMenuAdapter adapter = new SlideMenuAdapter(
				getApplicationContext(), RADIO_NAME);
		((ListView) findViewById(R.id.menu_listView)).setAdapter(adapter);
		((ListView) findViewById(R.id.menu_listView))
				.setOnItemClickListener(this);
		findViewById(R.id.main_layout).setOnTouchListener(this);
		cxt = this;

		mbradioPager = (CustomViewPager) findViewById(R.id.layout_pager);
		pagerAdapter = new MBradioPagerAdapter();
		findViewById(R.id.top_menu_play).setOnClickListener(pagerAdapter);
		findViewById(R.id.top_menu_mute).setOnClickListener(pagerAdapter);
		findViewById(R.id.top_menu_stop).setOnClickListener(pagerAdapter);

		findViewById(R.id.menu_listView).setOnTouchListener(this);
		findViewById(R.id.top_menu).setOnTouchListener(this);
		final View myV = findViewById(R.id.main_layout);
		final View menuV = findViewById(R.id.side_menu);
		// Toast.makeText(
		// getApplicationContext(),
		// "main_layout width: "
		// + getWindowManager().getDefaultDisplay().getWidth(),
		// Toast.LENGTH_LONG).show();
		int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				(int) (0.8 * screenWidth), LayoutParams.MATCH_PARENT,
				Gravity.RIGHT);
		menuV.setLayoutParams(params);
		mbradioPager
				.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

					// @Override
					public void onPageSelected(int index) {
						try {
							String str = mbradioPager.getChildAt(index)
									.getTag().toString();
							// Log.w("Radio PLayer Selected...", str);
							if (str.equals("Player Layout")) {
								findViewById(R.id.top_menu_player_butttons)
										.setVisibility(View.INVISIBLE);
							} else {
								findViewById(R.id.top_menu_player_butttons)
										.setVisibility(View.VISIBLE);
							}
						} catch (NullPointerException e) {

						}
						switch (index) {
						case 0:
							// LoadRadiosList();
							break;

						default:
							break;
						}
					}

					// @Override
					public void onPageScrolled(int arg0, float arg1, int arg2) {
						// TODO Auto-generated method stub

					}

					// @Override
					public void onPageScrollStateChanged(int arg0) {
						// TODO Auto-generated method stub

					}
				});

		mbradioPager.setAdapter(pagerAdapter);
//		mbradioPager.setCurrentItem(1);
		pagerAdapter.selectPlayerPage();
		findViewById(R.id.main_layout).setOnFocusChangeListener(
				new View.OnFocusChangeListener() {

					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						// Log.w("Radio Serveio Main Layout Focus changed",
						// "Focus has changed...");

					}
				});
		// mbradioPager.setOnClickListener(this);
		mbradioPager.setOnTouchListener(this);
		findViewById(R.id.top_menu).setClickable(true);
		findViewById(R.id.top_menu).setOnClickListener(this);

		if (showAdmobs) {
			findViewById(R.id.admob_banner).setVisibility(View.VISIBLE);
			InitAdMob();
		} else {
			findViewById(R.id.admob_banner).setVisibility(View.GONE);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (receiver != null)
				unregisterReceiver(receiver);
			// Log.w("Radio Serveio On Stop", "Saving data: " + RADIO_NAME +
			// " : "
			// + SET_VOLUME_LEVEL);
			Editor editor = getSharedPreferences("mypreferences", MODE_PRIVATE)
					.edit();
			editor.putString("last_selected_radio_name", RADIO_NAME);
			editor.putInt("last_volume_level", SET_VOLUME_LEVEL);
			editor.commit();
		} catch (NullPointerException e) {

		} catch (IllegalStateException e) {

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Log.w("Radio Serveio On Resume", "Resuming application...");

		if (menu_on && findViewById(R.id.main_layout).getLeft() == 0) {
			menu_on = false;
			if (mbradioPager != null)
				mbradioPager.setPagingEnabled(true);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// try {
		// Log.w("Radio Serveio On Stop", "Saving data: " + RADIO_NAME + " : " +
		// SET_VOLUME_LEVEL);
		// Editor editor = getSharedPreferences("mypreferences",
		// MODE_PRIVATE).edit();
		// editor.putString("last_selected_radio_name", RADIO_NAME);
		// editor.putInt("last_volume_level",SET_VOLUME_LEVEL);
		// editor.commit();
		// } catch (NullPointerException e) {
		//
		// }
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (!initialized)
				return;
			// Log.w("Radio Serveio On Stop", "Saving data: " + RADIO_NAME +
			// " : "
			// + SET_VOLUME_LEVEL);
			Editor editor = getSharedPreferences("mypreferences", MODE_PRIVATE)
					.edit();
			// editor.putString("last_selected_radio_name", RADIO_NAME);
			editor.putInt("last_volume_level", SET_VOLUME_LEVEL);
			editor.commit();

		} catch (NullPointerException e) {

		}
	}

	private void LoadLastValues() {
		SharedPreferences sp = getSharedPreferences("mypreferences",
				MODE_PRIVATE);
		RADIO_NAME = sp.getString("last_selected_radio_name", null);
		LAST_VOLUME_LEVEL = sp.getInt("last_volume_level", 20);
		if (LAST_VOLUME_LEVEL == 0)
			LAST_VOLUME_LEVEL = 20;
		RADIO_NAME_AUX = sp.getString("first_radio_name_in_list", null);
		// Log.e("Radio Serveion Load Values", RADIO_NAME + ": "
		// + LAST_VOLUME_LEVEL + ": " + RADIO_NAME_AUX);
		String aux = sp.getString("show_admob", "no").toLowerCase();
		if (aux.equals("yes"))
			showAdmobs = true;
		else
			showAdmobs = false;
		admobID = sp.getString("admob_id", null);
	}

	public boolean onTouch(View v, MotionEvent event) {

		if (event.getPointerCount() > 1) {
			// this will prevent two fingers events avoiding issue (layout
			// redraw)...
			return true;
		} else {
			// if (v.getId() == R.id.top_menu) {

			if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
				Log.i("onTouch",
						"" + v.getId() + " 0x"
								+ String.format("%08x", v.getId()));
				if (v.getId() == R.id.top_menu || menu_on
						&& (v.getId() == R.id.layout_pager)) {
					applyTheEvent(event, v);
					return true;
				}
			}
		}
		if (event.getActionMasked() == MotionEvent.ACTION_UP
				&& direction != null) {
			// applyTheEvent(event);
			// animateMenu();
			new MenuAnimation().execute(new String[] { direction });
			direction = null;
		}
		// }

		return false;
	}

	private void applyTheEvent(MotionEvent event, View _menuView) {
		// remove this 'if' statement to enable swipe left to show menu
		if (!menu_on)
			return;
		View myV = findViewById(R.id.main_layout);
		View menuView = findViewById(R.id.side_menu);
		int r = (int) event.getRawX();
		if (r < menuView.getLeft()) {
			r = menuView.getLeft();
			menu_on = true;
			mbradioPager.setPagingEnabled(false);
		}
		int l = r - myV.getWidth();

		myV.layout(l, myV.getTop(), r, myV.getBottom());
		// Log.w("Slide menu", "Left: " + myV.getLeft() + " Right: "
		// + myV.getRight());
		if (r > lastX + 25) {
			direction = "right";
			lastX = r;
			findViewById(R.id.top_menu).setClickable(false);
		} else if (r < lastX - 25) {
			direction = "left";
			lastX = r;
			findViewById(R.id.top_menu).setClickable(false);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (findViewById(R.id.main_layout) != null) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (menu_on) {
					findViewById(R.id.top_menu).performClick();
					return true;
				} else if (((pageItem) mbradioPager.getChildAt(
						mbradioPager.getCurrentItem()).getTag()).getTitle()
						.toLowerCase().contains("browser")) {
					if (((BrowserView) mbradioPager.getChildAt(
							mbradioPager.getCurrentItem()).findViewById(
							R.id.browser)).GoBack())
						return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		return true;
	}

	// @Override
	public void onClick(final View v) {

		if (menu_on && findViewById(R.id.main_layout).getLeft() == 0)
			menu_on = false;
		Animation bAnim = AnimationUtils.loadAnimation(v.getContext(),
				R.anim.buttons_animation);
		v.setAnimation(bAnim);
		bAnim.setAnimationListener(new Animation.AnimationListener() {

			// @Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			// @Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			// @Override
			public void onAnimationEnd(Animation animation) {

				if (v.getId() == R.id.top_menu && direction == null) {
					if (menu_on) {
						new MenuAnimation().execute(new String[] { "right" });

					} else {
						new MenuAnimation().execute(new String[] { "left" });

					}
				} else {
				}
			}
		});
		v.startAnimation(v.getAnimation());

	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position,
			long arg3) {
		if (!menu_on)
			return;
		SlideMenuItem item = (SlideMenuItem) v.getTag();
		if (item.getType() == SlideMenuItem.TYPE.SECTION)
			return;
		// Toast.makeText(getApplicationContext(), item.getTitle(),
		// Toast.LENGTH_LONG).show();
		String action = item.getTitle().toLowerCase();
		// int browserIndex = ((pageItem)
		// findViewById(R.id.browser_layout).getTag()).getIndex();
		if (action.contains("contact")) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "subject here");
			intent.putExtra(Intent.EXTRA_TEXT, "message here");
			startActivity(intent);
			// Intent mailer = Intent.createChooser(intent, null);
			// startActivity(mailer);
		} else if (action.contains("website") || action.contains("facebook")) {
			String url = item.getExtra();
			if (!url.contains("://"))
				url = "http://" + url;
			try {
				new MenuAnimation().execute(new String[] { "right" });
//				mbradioPager.setCurrentItem(browserPageIndex);
				pagerAdapter.selectBrowserPage();
				((BrowserView) findViewById(R.id.browser)).OpenUrl(url);

			} catch (NullPointerException e) {

			}
		} else if (action.contains("about ")) {
			Toast.makeText(getApplicationContext(), item.getExtra(),
					Toast.LENGTH_LONG).show();
		} else {
			String extra = item.getExtra();
			if (extra.contains("rssfeed://")) {
				try {
					//mbradioPager.setCurrentItem(browserPageIndex);
					pagerAdapter.selectBrowserPage();
					// ((BrowserView) findViewById(R.id.browser))
					// .LoadData("file:///android_asset/",
					// "<h3>Loading, please wait...</h3>");
					new MenuAnimation().execute(new String[] { "right" });
					String url = extra.split("feed://")[1];
					RssParser rss = new RssParser();
					String data = rss.xmlParsing(url, getApplicationContext());
					Log.i(getClass().toString(), data);
					((BrowserView) findViewById(R.id.browser)).OpenUrl(data,
							true);
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean haveNetworkConnection() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
	}

	private void InitAdMob() {

		if (admobID == null)
			return;
		// add Google AdMOb������
		AdView ad = new AdView(this, AdSize.BANNER, admobID);// "a14f9415c1664f3");
		ad.setGravity(Gravity.CENTER_HORIZONTAL);
		ad.setHorizontalGravity(Gravity.CENTER);
		((LinearLayout) findViewById(R.id.admob_banner)).addView(ad);
		ad.loadAd(new AdRequest());

		ad.setAdListener(new AdListener() {

			public void onReceiveAd(Ad arg0) {
				// TODO Auto-generated method stub

			}

			public void onPresentScreen(Ad arg0) {
				// TODO Auto-generated method stub

			}

			public void onLeaveApplication(Ad arg0) {
				// TODO Auto-generated method stub

			}

			public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
				arg0.loadAd(new AdRequest());
			}

			public void onDismissScreen(Ad arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	@SuppressWarnings("unused")
	private void LoadRadiosList() {
		ListView lv = (ListView) findViewById(R.id.radios_list);
		lv.setAdapter(new RadiosListAdapter(getApplicationContext()));
	}

	private void UpdateRadioMenu(String radioName) {
		SlideMenuAdapter adapter = new SlideMenuAdapter(
				getApplicationContext(), radioName);
		((ListView) findViewById(R.id.menu_listView)).setAdapter(adapter);
	}

	public class pageItem {
		private String title = null;
		private int myIdx = -1;

		public pageItem(String title, int index) {
			this.setTitle(title);
			this.setIndex(index);
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setIndex(int index) {
			this.myIdx = index;
		}

		public String getTitle() {
			return this.title;
		}

		public int getIndex() {
			return myIdx;
		}
	}

	private class MBradioPagerAdapter extends PagerAdapter implements
			OnClickListener, OnTouchListener {

		private ArrayList<Object> pages;
		private boolean isEnabled = true;
		private boolean hasHistory = false;
		private View playerView = null;
		private View browserView = null;

		public MBradioPagerAdapter() {
			pages = new ArrayList<Object>();
			LoadTrackHistory();
			LoadPlayerLayout();
			DbHelper dbh = new DbHelper(getApplicationContext());
			Cursor cursor = dbh.GetRadiosList();
			if (cursor.getCount() > 1)
				LoadRadiosLayout();// MAX_VIEWS = 3;
			cursor.close();
			dbh.close();
			LoadBrowserLayout();
		}

		// @Override
		public int getCount() {
			if (pages == null)
				return 0;
			else
				return pages.size();
		}

		@SuppressWarnings("unused")
		public void setEnabled(boolean val) {
			this.isEnabled = val;
		}

		public void selectPlayerPage() {
			for (int i = 0; i < pages.size(); i++) {
				if (pages.get(i).equals(playerView)) {
					mbradioPager.setCurrentItem(i);
					break;
				}
			}
		}

		private void selectBrowserPage(){
			for (int i = 0; i < pages.size(); i++) {
				if (pages.get(i).equals(browserView)) {
					mbradioPager.setCurrentItem(i);
					break;
				}
			} 
		}
		
		
//		public void removeTrackHistory() {
//			if (hasHistory) {
//				try {
//					mbradioPager.removeView((View) pages.get(0));
//					pages.remove(0);
//					hasHistory = false;
//					notifyDataSetChanged();
//				} catch (IndexOutOfBoundsException e) {
//
//				}
//			}
//		}

//		public View addTrackHistory() {
//			//return LoadTrackHistory();
//		}

		/**
		 * Create the page for the given position. The adapter is responsible
		 * for adding the view to the container given here, although it only
		 * must ensure this is done by the time it returns from
		 * {@link #finishUpdate()}.
		 * 
		 * @param container
		 *            The containing View in which the page will be shown.
		 * @param position
		 *            The page position to be instantiated.
		 * @return Returns an Object representing the new page. This does not
		 *         need to be a View, but can be some other container of the
		 *         page.
		 */
		// @Override
		public Object instantiateItem(View collection, int position) {
			View v = (View) pages.get(position);
			try {
				((ViewPager) collection).addView(v);
			} catch (IllegalStateException e) {

			}
			return v;
		}

		private View LoadTrackHistory() {
			View v = ((LayoutInflater) cxt
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.track_history_layout, null);
			// GridView gv = (GridView) v.findViewById(R.id.radios_grid);
			// ListAdapter la = new RadiosGridAdapter(cxt);
			// gv.setAdapter(la);
			v.setOnClickListener(this);
			v.setTag(new pageItem("Track History", pages.size()));
			if (!hasHistory) {
				hasHistory = true;
				if (pages.size() > 0)
					pages.add(0, v);
				else
					pages.add(v);
				notifyDataSetChanged();
			}
			return v;
		}

		private void LoadRadiosLayout() {
			View v = ((LayoutInflater) cxt
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.radios_layout, null);

			ListView lv = (ListView) v.findViewById(R.id.radios_list);
			lv.setAdapter(new RadiosListAdapter(getApplicationContext()));
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapter, View v,
						int position, long id) {
					if (menu_on)
						return; // disable any selection while slide menu is
								// open...
					String radio = v.getTag().toString();// ((TextView)
															// v).getText().toString();
					RADIO_NAME = radio;
					GetRadioData(radio, findViewById(R.id.player_layout));
					UpdateRadioMenu(radio);
					// mbradioPager.setCurrentItem(1);
					pagerAdapter.selectPlayerPage();
					UpdateSongInfo(null);
					if (IsServiceRunning()) {
						ShowProgressRing();
						SetAndSendBroadcast(ACTION.PLAY_STREAM.toString(),
								"stream", RADIO_URL);
						new ShoutcastTrackListHistory().execute(RADIO_URL);
					} else {
						StartPlayerService();
					}
				}
			});
			v.setOnClickListener(this);
			lv.setOnTouchListener(this);
			v.setTag(new pageItem("Radios List", pages.size()));
			pages.add(v);
			notifyDataSetChanged();
		}

		private void LoadBrowserLayout() {
			View v = ((LayoutInflater) cxt
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.browser_layout, null);
			v.setTag(new pageItem("Browser", pages.size()));
			browserPageIndex = pages.size();
			// Log.e("RadioServeIon", "browserPageIndex" + browserPageIndex);
			((BrowserView) v.findViewById(R.id.browser))
					.OpenUrl(DEFAULT_URL);//"http://mbradio.fm");
			pages.add(v);
			notifyDataSetChanged();
		}

		private int NameToInteger(String name) {
			String[] columns = { "name", "id", "mail", "about", "logo", "wurl",
					"phone", "rating", "rfbook" };
			for (int i = 0; i < columns.length; i++)
				if (columns[i].equals(name)) {
					return i;
				}
			return -1;
		}

		private String GetRadioStreams(String rid, DbHelper dbh) {
			String url;
			Cursor c = dbh.GetRadioStreams(rid);
			c.moveToFirst();
			url = c.getString(c.getColumnIndex("url"));
			c.close();
			return url;
		}

		private void GetRadioData(String name, View v) {
			DbHelper dbh = new DbHelper(getApplicationContext());
			Cursor c = dbh.GetRadioData(name);
			if (c.getCount() > 0) {
				c.moveToFirst();
				String aux;
				for (int i = 0; i < c.getColumnCount(); i++) {
					aux = c.getColumnName(i);
					switch (NameToInteger(aux)) {
					case 0: // name
						break;
					case 1: // get bitrates and urls
						RADIO_URL = GetRadioStreams(c.getString(i), dbh);
						
						// Log.w("Radio Stream", "" + RADIO_URL);
						// update track history

						break;
					case 2: // mail
						break;
					case 3: // about
						break;
					case 4: // logo
						aux = c.getString(i);
						String pathName = getExternalFilesDir("resources")
								+ aux;
						defaultCover = pathName;
						// Log.w("Radio Stream", "" + pathName);
						((ImageView) v.findViewById(R.id.imageView1))
								.setImageBitmap(BitmapFactory
										.decodeFile(pathName));
						break;
					case 5: // wurl
						DEFAULT_URL = c.getString(i);
						break;
					case 6: // phone
						break;
					case 7: // rating
						aux = c.getString(i);
						// rate is from 0 to 5 we need it from 0 to 100
						float rate = Float.parseFloat(aux);
						int progress = (int) (rate * 100.0f / 5.0f);
						((RatingBar) v.findViewById(R.id.ratingBar1))
								.setProgress(progress);
						break;
					case 8: // rfbook
						break;
					default:
						break;
					}
				}
			} else {
				// Log.w("Serveion Radio Player Layout", "No data found to: "
				// + name);
			}
			c.close();
			dbh.close();
		}

		private void LoadPlayerLayout() {
			View v = ((LayoutInflater) cxt
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.player_layout, null);
			// v.findViewById(R.id.now_playing).setSelected(true);
			v.findViewById(R.id.player_mute).setOnClickListener(this);
			v.findViewById(R.id.player_play).setOnClickListener(this);
			v.findViewById(R.id.player_stop).setOnClickListener(this);
			artist = ((TextView) v.findViewById(R.id.artist_name));
			title = ((TextView) v.findViewById(R.id.song_title));
			((SeekBar) v.findViewById(R.id.volume_bar))
					.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							SetAndSendBroadcast(ACTION.VOLUME.toString(),
									"value", progress);
							SET_VOLUME_LEVEL = progress;
						}
					});
			receiver = new mbReceiver();
			IntentFilter filter = new IntentFilter("mbradiobroadcast");
			registerReceiver(receiver, filter);
			((SeekBar) v.findViewById(R.id.volume_bar))
					.setProgress(LAST_VOLUME_LEVEL);
			// Log.w("Serveion Radio Player Layout", RADIO_NAME + " or "
			// + RADIO_NAME_AUX);
			if (RADIO_NAME != "" && RADIO_NAME != null) {
				// Log.w("Serveion Radio Player Layout", "RADIO_NAME:"
				// + RADIO_NAME);
				GetRadioData(RADIO_NAME, v);
			} else if (RADIO_NAME_AUX != null) {
				// Log.w("Serveion Radio Player Layout", "RADIO_NAME_AUX:"
				// + RADIO_NAME_AUX);
				GetRadioData(new String(RADIO_NAME_AUX), v);
			}

			if (IsServiceRunning()) {
				// Log.w("Serveion Radio Player Layout", "Url: " + RADIO_URL);
				SetAndSendBroadcast(ACTION.PLAY_STREAM.toString(), "stream",
						RADIO_URL);
				// perform
				SetAndSendBroadcast(ACTION.UPDATE_SONG_INFO.toString(), "", 0);
				// new ShoutcastTrackListHistory().execute(RADIO_URL);
				// SetAndSendBroadcast(ACTION.UPDATE_TRACKLIST.toString(), "",
				// 0);
			} else {
				// StartPlayerService();
			}
			v.setTag(new pageItem("Payer", pages.size()));
			playerView = v;
			pages.add(v);
			notifyDataSetChanged();
		}

		public boolean onTouch(View v, MotionEvent event) {
			if (event.getPointerCount() > 1) {
				// Toast.makeText(getApplicationContext(),
				// "Two fingers event... ", Toast.LENGTH_LONG).show();
				return true;
			}
			return false;
		}

		// @Override
		public void onClick(View v) {
			if (menu_on)
				return; // disable any funtion while menu is on...
			Animation bAnim = AnimationUtils.loadAnimation(v.getContext(),
					R.anim.buttons_animation);
			v.setAnimation(bAnim);
			v.startAnimation(bAnim);
			if (v.getId() == R.id.player_play
					|| v.getId() == R.id.top_menu_play) {
				StartPlayerService();
			} else if (v.getId() == R.id.player_stop
					|| v.getId() == R.id.top_menu_stop) {
				StopPlayerService();
//				pagerAdapter.removeTrackHistory();
			} else if (v.getId() == R.id.player_mute
					|| v.getId() == R.id.top_menu_mute) {
				SetAndSendBroadcast(ACTION.MUTE.toString(), "null", 0);
			} else {
			}
		}

		/**
		 * Remove a page for the given position. The adapter is responsible for
		 * removing the view from its container, although it only must ensure
		 * this is done by the time it returns from {@link #finishUpdate()}.
		 * 
		 * @param container
		 *            The containing View from which the page will be removed.
		 * @param position
		 *            The page position to be removed.
		 * @param object
		 *            The same object that was returned by
		 *            {@link #instantiateItem(View, int)}.
		 */
		// @Override
		public void destroyItem(View collection, int position, Object view) {
			// ((ViewPager) collection).removeView((View) view);
		}

		// @Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((View) object);
		}

		/**
		 * Called when the a change in the shown pages has been completed. At
		 * this point you must ensure that all of the pages have actually been
		 * added or removed from the container as appropriate.
		 * 
		 * @param container
		 *            The containing View which is displaying this adapter's
		 *            page views.
		 */

		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}

	}

	public class GetCover extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			GetMyCover(params[0], params[1]);
			return null;
		}

		@Override
		protected void onPreExecute() {
			((ImageView) findViewById(R.id.imageView1))
					.setImageBitmap(BitmapFactory.decodeFile(defaultCover));
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (values[0] != null)
				((ImageView) findViewById(R.id.imageView1))
						.setImageBitmap(BitmapFactory.decodeFile(values[0]));
		}

		private void GetMyCover(String artist, String track) {
			String url = getString(R.string.lastfm_api_url)
					+ "?method=track.getInfo&format=json&api_key="
					+ getString(R.string.lastfm_api_key) + "&artist=" + artist
					+ "&track=" + track;
			url = url.replace(" ", "%20");
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet();
				request.setURI(new URI(url));

				String remoteJson = client.execute(request,
						new BasicResponseHandler());
				// Log.i("remoteJson", remoteJson);
				remoteJson = new String(remoteJson.getBytes(), "UTF-8");
				// Log.e("remoteJson", remoteJson);
				JSONObject json = new JSONObject(remoteJson);
				if (json.has("ERROR")) {
					publishProgress(new String[] { null });
				} else if (json.has("track")) {
					// Log.e("remoteJson", "has track");
					json = json.getJSONObject("track");
					if (json.has("album")) {
						json = json.getJSONObject("album");
						if (json.has("image")) {
							// Log.e("remoteJson", "has image");
							JSONArray ja = json.getJSONArray("image");
							for (int i = 0; i < ja.length(); i++) {
								if (ja.getJSONObject(i).getString("size")
										.equals("large")) {
									String img_url = ja.getJSONObject(i)
											.getString("#text");
									// Log.e("remoteJson", remoteJson);
									String res = Download(img_url);
									publishProgress(new String[] { res });
								}
							}
						}
					}
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				// any cleanup code...
			}
		}

		public String Download(String link) {
			String filename = "";
			long remoteLastModified = 0;
			try {

				// set the download URL, a url that points to a file on the
				// internet
				// this is the file to be downloaded
				URL url = new URL(link.replace(" ", "%20"));

				// Log.e("Download file", this.link);
				// create the new connection
				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();

				// set up some things on the connection
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
				// and connect!
				urlConnection.connect();
				// set the path where we want to save the file
				// in this case, going to save it on the root directory of the
				// sd card.
				File externaStorage = getExternalFilesDir("resources");
				// create a new file, specifying the path, and the filename
				// which we want to save the file as.
				String ext = MimeTypeMap.getFileExtensionFromUrl(link);
				filename = "cover." + ext;
				// Log.e("COVER FILE NAME", filename);
				File file = new File(externaStorage, filename);

				if (!file.exists()) {
					file.createNewFile();
				}

				// this will be used to write the downloaded data into the file
				// we created
				FileOutputStream fileOutput = new FileOutputStream(file);

				// this will be used in reading the data from the internet
				InputStream inputStream = urlConnection.getInputStream();

				// this is the total size of the file
				int totalSize = urlConnection.getContentLength();
				// variable to store total downloaded bytes
				int downloadedSize = 0;

				// " "), totalSize);
				// create a buffer...
				byte[] buffer = new byte[1024];
				int bufferLength = 0; // used to store a temporary size of the
										// buffer
				int percentage = 0;
				int lastPercentage = 0;

				// Log.i("SplashActivity", "Start downloading " + filename +
				// "");
				// now, read through the input buffer and write the contents to
				// the
				// file
				while ((bufferLength = inputStream.read(buffer)) > 0) {
					// add the data in the buffer to the file in the file output
					// stream (the file on the sd card
					fileOutput.write(buffer, 0, bufferLength);
					// add up the size so we know how much is downloaded
					downloadedSize += bufferLength;
					// this is where you would do something to report the
					// progress,
					// like this maybe
					// Only update each 2% steps...
					percentage = (int) (((float) downloadedSize / (float) totalSize) * 100.00f);
					if (percentage != lastPercentage) {// percentage % 2 == 0 &&
														// percentage !=
														// lastPercentage) {
														// lastPercentage =
														// percentage;
						// Log.w("Progress percentage",
						// Integer.toString(percentage)
						// + "%");
					}
				}

				// close the output stream when done
				fileOutput.close();
				file.setLastModified(remoteLastModified);
				// Log.w("File Name", filepath);
				return file.getAbsolutePath();

				// catch some possible errors...
			} catch (MalformedURLException e) {
				filename = "";
				e.printStackTrace();
			} catch (IOException e) {
				filename = "";
				e.printStackTrace();
			}
			return null;

		}
	}

	public class MenuAnimation extends AsyncTask<String, Integer, Void> {
		private boolean end = false;
		private final long INTERVAL = 40000; // in nanoseconds

		@Override
		protected Void doInBackground(String... params) {
			int step = -1;
			if (params[0].equals("right")) {
				step = -step;
			}
			StartAnim(step);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			View m, s;
			m = findViewById(R.id.main_layout);
			s = findViewById(R.id.side_menu);
			int step, l, r;
			step = (int) values[0];
			l = m.getLeft() + step;
			r = m.getRight() + step;
			if (step > 0) {
				if (l >= 0) {
					l = 0;
					r = m.getWidth();
					end = true;
					menu_on = false;
					mbradioPager.setPagingEnabled(true);
					findViewById(R.id.top_menu).setClickable(true);
				}
			} else {
				if (r <= s.getLeft()) {
					r = s.getLeft();
					l = r - m.getWidth();
					end = true;
					menu_on = true;
					mbradioPager.setPagingEnabled(false);
					findViewById(R.id.top_menu).setClickable(true);
				}
			}
			m.layout(l, m.getTop(), r, m.getBottom());

		}

		private void StartAnim(int step) {
			long start = System.nanoTime();
			Integer[] value = new Integer[1];
			value[0] = step;
			while (!end) {
				if (System.nanoTime() > start + INTERVAL) {
					publishProgress(value);
					start = System.nanoTime();
				}
			}
		}
	}

	public class ShoutcastTrackListHistory extends
			AsyncTask<String, Void, String> {

		private String url;

		@Override
		protected String doInBackground(String... params) {
			try {
				this.url = params[0] + "/played.html";
				HttpClient httpclient = new DefaultHttpClient();
				String ua = "Mozilla/4.0 (compatible; MSIE 5.5; Windows 98)";
				httpclient.getParams().setParameter(
						CoreProtocolPNames.USER_AGENT, ua);
				HttpGet httpget = new HttpGet(this.url);
				HttpResponse response = httpclient.execute(httpget); // Executeit
				HttpEntity entity = response.getEntity();
				// Create an InputStream with the response
				InputStream is = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "utf-8"), 64);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null)
					// Read line by line
					sb.append(line + "\n");

				String resString = sb.toString(); // Result is here

				is.close(); // Close the stream
				resString = ParseList(resString);
				return resString;
			} catch (IllegalArgumentException iae) {

			} catch (ClientProtocolException cpe) {

			} catch (IOException ioe) {

			} catch (Exception e) {

			}
			return null;
		}

		private String ParseList(String html) {
			String list = "";

			if (!html.contains("SHOUTcast")) {
				return null;
			}

			String tableStart = "<table border=0 cellpadding=2 cellspacing=2>";
			String tableEnd = "</table>";

			list = html.substring(html.indexOf(tableStart)
					+ tableStart.length());
			list = list.substring(0, list.indexOf(tableEnd));
			String[] rows = list.split("</tr><tr>");
			int l = 0;
			String result = "";
			for (String s : rows) {
				if (l > 0) {
					result += s.split("</td><td>")[1];
					try {
						result = result.substring(0, result.indexOf("<td>"));
					} catch (IndexOutOfBoundsException iobe) {

					}
					result = result.replace("</tr>", "").replace(
							"Romantic Jazz -", "");
					result += "\n";
				}
				l++;
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
//				pagerAdapter.removeTrackHistory();
				// ((ListView) findViewById(R.id.track_list)).setAdapter(null);
				return;
			}
			String[] list = result.split("\n");
			// Log.w("AsynchTrackHistory", "" + list.length);
			if (list.length == 0) {
//				pagerAdapter.removeTrackHistory();
				return;
			}
			TrackListAdapter tla = new TrackListAdapter(
					getApplicationContext(), list);
			try {
//				View v = pagerAdapter.addTrackHistory();
				((ListView) findViewById(R.id.track_list)).setAdapter(tla);
			} catch (NullPointerException e) {

			}
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onProgressUpdate(Void... values) {

		}
	}

	private void StartPlayerService() {
		new ShoutcastTrackListHistory().execute(RADIO_URL);
		// Log.w("MBRadio.fm", "Starting service...");
		Intent intent = new Intent(RadioServeionActivity.this,
				PlayerService.class);
		intent.putExtra("stream", RADIO_URL);
		int volume = LAST_VOLUME_LEVEL;
		try {
			volume = ((SeekBar) findViewById(R.id.volume_bar)).getProgress();
		} catch (NullPointerException e) {

		}
		intent.putExtra("audio_level", volume);
		startService(intent);
	}

	private void ShowProgressRing() {
		findViewById(R.id.loadingProgressBar).setVisibility(View.VISIBLE);
	}

	private void HideProgressRing() {
		findViewById(R.id.loadingProgressBar).setVisibility(View.INVISIBLE);
	}

	private void StopPlayerService() {
		stopService(new Intent(RadioServeionActivity.this, PlayerService.class));
		artist.setText(" - ");
		title.setText(" - ");
	}

	private boolean IsServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("radio.serveion.com.PlayerService".equals(service.service
					.getClassName())) {
				// Log.e("PlayerService", "PlayerService found: "
				// + service.service.toString());
				return true;
			}
		}
		// Log.e("PlayerService", "Player Service not found");
		return false;
	}

	private void SetAndSendBroadcast(String action, String extraName,
			String extraValue) {
		Intent intent = new Intent("smbradiobroadcast");
		intent.putExtra("action", action);
		intent.putExtra(extraName, extraValue);
		sendBroadcast(intent);
	}

	private void SetAndSendBroadcast(String action, String extraName,
			int extraValue) {
		Intent intent = new Intent("smbradiobroadcast");
		intent.putExtra("action", action);
		intent.putExtra(extraName, extraValue);
		sendBroadcast(intent);
	}

	private void UpdateSongInfo(String song) {
		if (artist != null && title != null) {
			if (song == null) {
				// Log.w("RTuga activity", "song info is null");
				artist.setText(" ? ");
				artist.setSelected(true);
				title.setText(" ? ");
				title.setSelected(true);
				return;
			}
			// Log.w("RTuga activity", song);
			String[] data = song.split("-");
			if (data.length > 1) {
				artist.setText(data[0]);
				artist.setSelected(true);
				title.setText(data[1]);
				title.setSelected(true);
				new GetCover().execute(data);
			} else {
				artist.setText("");
				artist.setSelected(true);
				title.setText(song);
				title.setSelected(true);
			}
		}
		new ShoutcastTrackListHistory().execute(RADIO_URL);
	}

	public class mbReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("mbradiobroadcast")) {
				String action = intent.getStringExtra("action");
				// Log.w("RTuga activity", action);
				if (action.equals(ACTION.UPDATE_SONG_INFO.toString())) {
					UpdateSongInfo(intent.getStringExtra("song"));
				} else if (action.equals(ACTION.UPDATE_STATUS.toString())) {
					if (!intent.hasCategory("status"))
						return;
					String value = intent.getStringExtra("status");
					if (value.equals("NOW PLAYING:")
							|| value.equals("NOW MUTING:")) {
						// findViewById(R.id.player_nowplaying_label)
						// .setVisibility(View.VISIBLE);
						// findViewById(R.id.active_radio_layout).setVisibility(
						// View.VISIBLE);
						if (value.equals("NOW MUTING:")) {
							// ((ImageView) findViewById(R.id.player_mute))
							// .setImageResource(android.R.drawable.ic_lock_silent_mode);
						} else {
							// ((ImageView) findViewById(R.id.player_mute))
							// .setImageResource(android.R.drawable.ic_lock_silent_mode_off);
						}

					}
					// else
					// findViewById(R.id.player_nowplaying_text)
					// .setVisibility(View.GONE);
					// ((TextView) findViewById(R.id.player_nowplaying_label))
					// .setText(value);
				} else if (action.equals(ACTION.SHOW_LOADING.toString())) {
					ShowProgressRing();
				} else if (action.equals(ACTION.HIDE_LOADING.toString())) {
					HideProgressRing();
				} else if (action.equals(ACTION.UPDATE_TRACKLIST.toString())) {
					if (!intent.hasCategory("track_list"))
						return;
					ArrayList<String> list = intent
							.getStringArrayListExtra("track_list");
					// UpdateTrackHistoryList(list);

				}
			}
		}
	}

}