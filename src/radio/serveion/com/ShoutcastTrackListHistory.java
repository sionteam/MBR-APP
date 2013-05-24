package radio.serveion.com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.os.AsyncTask;

public class ShoutcastTrackListHistory extends AsyncTask<String, Void, String> {

	private String url;

	@Override
	protected String doInBackground(String... params) {
		try {
			this.url = params[0] + "/played.html";
			HttpClient httpclient = new DefaultHttpClient();
			String ua = "Mozilla/4.0 (compatible; MSIE 5.5; Windows 98)";
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
					ua);
			HttpGet httpget = new HttpGet(this.url);
			HttpResponse response = httpclient.execute(httpget); // Executeit
			HttpEntity entity = response.getEntity();
			// Create an InputStream with the response
			InputStream is = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "utf-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
				// Read line by line
				sb.append(line + "\n");

			String resString = sb.toString(); // Result is here

			is.close(); // Close the stream
			return resString;
		} catch (IllegalArgumentException iae) {

		} catch (ClientProtocolException cpe) {

		} catch (IOException ioe) {

		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		
	}

	@Override
	protected void onPreExecute() {

	}

	@Override
	protected void onProgressUpdate(Void... values) {

	}
}
