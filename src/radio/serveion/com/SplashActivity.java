package radio.serveion.com;

import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent.OnFinished;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Directory;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity implements View.OnClickListener {

	// status text view - show user what it happening...
	private TextView status;

	// private String userCode = "67c6a1e7ce56d3d6fa748ab6d9af3fd7";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// extra code 3988c7f88ebcb58c6ce932b957b6f332
		// mbuser code 67c6a1e7ce56d3d6fa748ab6d9af3fd7
		Log.i("Serveion Radio", "Splash creating...");
		setContentView(R.layout.splash_layout);
		LoadSplashImage();
		status = (TextView) findViewById(R.id.status_view);
		findViewById(R.id.update_cancel).setOnClickListener(this);
		findViewById(R.id.update_ok).setOnClickListener(this);

		String[] params = { "check",
				getText(R.string.server_url) + "/API/?act=vapk&pkg=" };
		new AppUpdate().execute(params);
	}

	private void LoadAppData() {
		String[] params = {
				getText(R.string.server_url) + "/API/?act=uappinfo&c="
						+ getText(R.string.user_code),
				getText(R.string.server_url) + "/" };

		new SplashAsync().execute(params);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.update_cancel) {
			HideButtons();
			LoadAppData();
		} else if (v.getId() == R.id.update_ok) {
			HideButtons();
			String url = v.getTag().toString();
			String[] params = { "update", url };
			new AppUpdate().execute(params);
		}

	}

	private void LoadSplashImage() {
		SharedPreferences sp = getSharedPreferences("mypreferences",
				MODE_PRIVATE);
		String pathName = sp.getString("splash_image", null);
		if (pathName == null)
			return;
		Log.e("Splash ...", "Slpash background: " + pathName);
		View v = findViewById(R.id.splash_layout);
		Drawable d = Drawable.createFromPath(pathName);
		v.setBackgroundDrawable(d);
	}

	private void HideButtons() {
		findViewById(R.id.update_cancel).setVisibility(View.GONE);
		findViewById(R.id.update_ok).setVisibility(View.GONE);
		findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
	}

	public void FinishWithOkResult() {
		Intent i = new Intent();
		i.putExtra("result", "result");
		if (getParent() != null)
			getParent().setResult(RESULT_OK, i);
		else
			setResult(RESULT_OK, i);
		finish();
	}

	private void CancellApp() {
		Intent i = new Intent();
		i.putExtra("result", "result");
		if (getParent() != null)
			getParent().setResult(RESULT_CANCELED, i);
		else
			setResult(RESULT_CANCELED, i);
		finish();
	}

	private void InstallUpdate(String file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(file)),
				"application/vnd.android.package-archive");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag
														// android returned a
														// intent error!
		getApplicationContext().startActivity(intent);
		CancellApp();
	}

	public class SplashAsync extends AsyncTask<String, String, String> {

		private String baseUrl = "";
		private String admob_id, show_admob, bkg_color, bkg_image, splashImage,
				feedNames, feedUrls;

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			this.baseUrl = params[1];
			GetJSONData(url);
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (values[0].equals("new_splash")) {
				LoadSplashImage();
				return;
			} else if (values.length > 1) {
				String message = values[0];
				int progress = Integer.parseInt(values[1]);
				if (findViewById(R.id.progress_horizontal).getVisibility() == View.INVISIBLE)
					findViewById(R.id.progress_horizontal).setVisibility(
							View.VISIBLE);
				((TextView) findViewById(R.id.status_view)).setText(message);
				((ProgressBar) findViewById(R.id.progress_horizontal))
						.setProgress(progress);
				if (progress >= 100)
					findViewById(R.id.progress_horizontal).setVisibility(
							View.VISIBLE);
			} else {
				((TextView) findViewById(R.id.status_view)).setText(values[0]);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Log.i("Serveion Radio", "Splash closing and send request result");
			FinishWithOkResult();
		}

		private void GetJSONData(String url) {
			String savedJson = GetSavedJsonData();
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet();
				request.setURI(new URI(url));

				String remoteJson = client.execute(request,
						new BasicResponseHandler());
				// Log.i("remoteJson", remoteJson);
				remoteJson = new String(remoteJson.getBytes(), "UTF-8");
				// Log.i("remoteJson", remoteJson);
				if (!remoteJson.equals(savedJson)) {
					Log.i("remoteJson", "JSON data has changed");
					publishProgress(new String[] { "Found data to update." });
					SaveJasonData(remoteJson);
					UpdateData(remoteJson);
				} else {
					Log.i("remoteJson", "JSON data is up to date");
					// UpdateData(savedJson);
					publishProgress(new String[] { "Nothing to update." });
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				// any cleanup code...
			}

		}

		private String GetSavedJsonData() {
			String str = "";
			InputStreamReader reader;
			try {
				reader = new InputStreamReader(getApplicationContext()
						.openFileInput("json_data.txt"));

				BufferedReader br = new BufferedReader(reader);
				String line;
				while ((line = br.readLine()) != null) {
					str += line;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return str;
		}

		private void SaveJasonData(String JsonStr) {
			OutputStreamWriter out;
			try {
				out = new OutputStreamWriter(getApplicationContext()
						.openFileOutput("json_data.txt", 0));

				out.write(JsonStr);
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void UpdateData(String json) {
			final String radios = "app_radios_list";
			DbHelper dbh = new DbHelper(getApplicationContext());
			dbh.ClearRadios();
			JSONArray radiosList;
			try {
				JSONObject jObj = new JSONObject(json);
				JSONObject app_info = jObj.getJSONObject("app_info");
				admob_id = app_info.getString("admob_id");
				show_admob = app_info.getString("show_admob");
				bkg_color = app_info.getString("bkg_color");
				bkg_image = app_info.getString("bkg_image");
				feedNames = "";
				feedUrls = "";
				for (int i = 1; i <= 3; i++) {
					String jname = "feedname" + i;
					String jname1 = "feed" + i;
					Log.e("Feeds", jname + " $$ " + jname1);
					try {
						if (i > 1) {
							feedNames += "~~~";
							feedUrls += "~~~";
						}
						feedNames += app_info.getString(jname.trim()).replace(
								"null", "");
						feedUrls += app_info.getString(jname1.trim()).replace(
								"null", "");
					} catch (JSONException e) {
						feedNames += "";
						feedUrls += "";
					}
				}
				Log.e("Feeds", feedNames + " <<<>>>" + feedUrls);
				if (bkg_image != "" && bkg_image != null) {
					bkg_image = Download(baseUrl + bkg_image, "backgroud");
					bkg_image = getExternalFilesDir("resources") + bkg_image;
				}
				splashImage = app_info.getString("splash_image");
				if (splashImage != "" && splashImage != null) {
					splashImage = Download(splashImage, "splash");
					splashImage = getExternalFilesDir("resources")
							+ splashImage;
				}
				SetSharedPreferences();
				publishProgress(new String[] { "new_splash" });
				String aux;
				radiosList = jObj.getJSONArray(radios);
				// Store radio data into database
				String[] values = new String[10];
				JSONObject nowJsonObject;
				for (int i = 0; i < radiosList.length(); i++) {
					Log.i("Radio Name",
							radiosList.getJSONObject(i).getString("name"));
					nowJsonObject = radiosList.getJSONObject(i);
					values[0] = nowJsonObject.getString("name");
					values[1] = nowJsonObject.getString("mail");
					values[2] = nowJsonObject.getString("about");
					// TODO: LOGO must be verified and download
					values[3] = nowJsonObject.getString("logo");

					aux = baseUrl + values[3];
					aux = aux.replace(".png", "_250.png");
					aux = values[3] = Download(aux, null);

					// *************************************//
					values[4] = nowJsonObject.getString("wurl");
					values[5] = nowJsonObject.getString("phone");
					values[6] = nowJsonObject.getString("rating");
					values[7] = nowJsonObject.getString("rfbook");
					values[8] = ComposeGenres(nowJsonObject
							.getJSONArray("genre"));
					values[9] = nowJsonObject.getString("lang");
					dbh.InsertRadioData(values);
					if (i == 0) {
						getSharedPreferences("mypreferences", MODE_PRIVATE)
								.edit();
						Editor editor = getSharedPreferences("mypreferences",
								MODE_PRIVATE).edit();
						Log.w("Splash Activity",
								"Writing default radio to preferences: "
										+ values[0]);
						editor.putString("first_radio_name_in_list", values[0]);
						editor.commit();
					}
					InsertStreamsUrls(nowJsonObject.getJSONArray("streams"),
							dbh);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			dbh.close();
		}

		private void InsertStreamsUrls(JSONArray streams, DbHelper dbh) {
			try {
				String[] values = new String[3];
				values[0] = dbh.getLastId();
				for (int i = 0; i < streams.length(); i++) {
					values[1] = streams.getJSONObject(i).getString("bitrate");
					values[2] = streams.getJSONObject(i).getString("url");
					dbh.InsertRadioStream(values);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		private String ComposeGenres(JSONArray genres) {
			String g = "";
			for (int i = 0; i < genres.length(); i++) {
				if (i > 0)
					g += "|";
				try {
					g += genres.getString(i);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return g;
		}

		private void SetSharedPreferences() {
			SharedPreferences sp = getSharedPreferences("mypreferences",
					MODE_PRIVATE);
			Editor editor = sp.edit();
			editor.putString("admob_id", admob_id);
			editor.putString("show_admob", show_admob);
			editor.putString("bkg_color", bkg_color);
			editor.putString("bkg_image", bkg_image);
			editor.putString("splash_image", splashImage);
			editor.putString("feed_name", feedNames);
			editor.putString("feed_urls", feedUrls);
			editor.commit();
		}

		/**
		 * This method will download the requested file and place it on external
		 * storage card.
		 * 
		 * @param Url
		 *            Link to file to download.
		 * @return A string that represents the full path to download file.
		 */
		public String Download(String link, String fName) {
			String filename = "";
			long remoteLastModified = 0, localLastModified = 0;
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
				if (!externaStorage.exists())
					externaStorage.mkdirs();
				// create a new file, specifying the path, and the filename
				// which we want to save the file as.
				if (fName != null) {
					filename = "/"
							+ fName
							+ "."
							+ MimeTypeMap.getFileExtensionFromUrl(url
									.toString());
				} else {
					String suffix = link.substring(0, link.lastIndexOf('/'));
					suffix = suffix.substring(suffix.lastIndexOf('/')).replace(
							"%20", " ");
					filename = suffix
							+ "."
							+ MimeTypeMap.getFileExtensionFromUrl(url
									.toString());

					// link.substring(link.lastIndexOf('/') + 1)
					// .replace("%20", " ");
					// filename = "/" + name.replace("\\",
					// "").replace("/","").replace(" ","") + "_" +
					// filename.replace("/", "").replace(" ","");
					// filename =
					// filename.replace("ñ","n").replace("ã","a").replace("õ",
					// "o");
					Log.i("Local filename:", "" + filename);
				}
				File file = new File(externaStorage, filename);

				if (!file.exists()) {
					file.createNewFile();
				} else {
					localLastModified = file.lastModified();
					remoteLastModified = urlConnection.getLastModified();
					Log.i("SplashActivity Last Modified", "remote "
							+ remoteLastModified + "; local "
							+ localLastModified);
					if (localLastModified == remoteLastModified) {
						// nothing to do abort
						Log.i("SplashActivity", "File " + filename
								+ " is up to date");
						return filename;
					}
				}

				// this will be used to write the downloaded data into the file
				// we
				// created
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

				Log.i("SplashActivity", "Start downloading " + filename + "");
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
					if (percentage != lastPercentage) {
						publishProgress(new String[] { "Downloading file...",
								"" + percentage });
					}
				}

				// close the output stream when done
				fileOutput.close();
				file.setLastModified(remoteLastModified);
				// Log.w("File Name", filepath);

				// catch some possible errors...
			} catch (MalformedURLException e) {
				filename = "";
				e.printStackTrace();
			} catch (IOException e) {
				filename = "";
				e.printStackTrace();
			}
			// Log.i("filepath:", " " + filepath);
			return filename;

		}
	}

	public class AppUpdate extends AsyncTask<String, String, String> {

		private String vName, vCode, pkg, action;

		@Override
		protected String doInBackground(String... params) {
			String result = "";
			action = params[0];
			if (action.equals("check")) {
				GetMyVersion();
				result = CheckRemoteVersion(params[1] + pkg);
			} else if (action.equals("update")) {
				result = DownLoadAndInstall(params[1]);
			}
			return result;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (action.equals("update")) {
				if (findViewById(R.id.progress_horizontal).getVisibility() != View.VISIBLE)
					findViewById(R.id.progress_horizontal).setVisibility(
							View.VISIBLE);
				((TextView) findViewById(R.id.status_view)).setText(values[0]);
				((ProgressBar) findViewById(R.id.progress_horizontal))
						.setProgress(Integer.parseInt(values[1]));
			} else if (action.equals("check")) {
				if (values[0].equals("new_version_available")) {
					((TextView) findViewById(R.id.status_view))
							.setText("A new version of App is available...");
					findViewById(R.id.update_ok).setVisibility(View.VISIBLE);
					findViewById(R.id.update_ok).setTag(values[1]);
					findViewById(R.id.update_cancel)
							.setVisibility(View.VISIBLE);
					((ProgressBar) findViewById(R.id.progress_horizontal))
							.setProgress(0);
					// findViewById(R.id.progress_horizontal).setVisibility(
					// View.VISIBLE);
				}
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result.equals("no_update")) {
				LoadAppData();
			} else if (result.contains("INSTALL:")) {
				String[] values = result.split("ALL: ");
				InstallUpdate(values[1]);
			}
		}

		private void GetMyVersion() {
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				pkg = pi.packageName;
				vCode = "" + pi.versionCode;
				vName = pi.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}

		private String CheckRemoteVersion(String url) {
			String res = "no_update";
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet();
				request.setURI(new URI(url));

				String remoteJson = client.execute(request,
						new BasicResponseHandler());
				// Log.i("remoteJson", remoteJson);
				remoteJson = new String(remoteJson.getBytes(), "UTF-8");
				String rCode, rName;
				JSONObject jo = new JSONObject(remoteJson);
				if (!jo.has("ERROR")) {
					rCode = jo.getString("vCode");
					rName = jo.getString("vName");
					if (!vCode.equals(rCode) && !vName.equals(rName)) {
						res = jo.getString("apkUrl");
						publishProgress(new String[] { "new_version_available",
							getString(R.string.server_url) + "/" + res });
					}
				} else {
					Log.e("PACKAGE UPDATE", remoteJson);
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
			return res;
		}

		private String DownLoadAndInstall(String _url) {
			String filename = "";
			try {

				// set the download URL, a url that points to a file on the
				// internet
				// this is the file to be downloaded
				URL url = new URL(_url.replace(" ", "%20"));

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
				File externalStorage = Environment
						.getExternalStorageDirectory();// getExternalFilesDir("download");
				// create a new file, specifying the path, and the filename
				// which we want to save the file as.
				filename = "/download/update.apk";
				File file = new File(externalStorage, filename);

				if (!file.exists()) {
					file.createNewFile();
				}

				// this will be used to write the downloaded data into the file
				// we
				// created
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
				String[] updater = new String[2];
				updater[0] = "Downloading new version ...";
				Log.i("SplashActivity", "Start downloading " + filename + "");
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
					if (percentage % 2 == 0 && percentage != lastPercentage) {
						lastPercentage = percentage;
						updater[1] = "" + percentage;
						publishProgress(updater);
					}
				}

				// close the output stream when done
				fileOutput.close();
				filename = "INSTALL: " + file.getAbsolutePath();
				// catch some possible errors...
			} catch (MalformedURLException e) {
				filename = "";
				e.printStackTrace();
			} catch (IOException e) {
				filename = "";
				e.printStackTrace();
			}
			return filename;
		}
	}
}
