package at.tomtasche.nomstagram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class HelloAndroidActivity extends Activity {

	private String API_URL_ENDPOINT = "https://api.foursquare.com/v2/";
	private String API_URL_METHOD_SEARCH = "venues/search";
	private String API_URL_METHOD_PHOTOS = "venues/%s/photos";

	private String API_PARAM_VERSION = "?v=20140712";
	private String API_PARAM_CLIENT_ID = "&client_id=H4ZMXXNEL2HFTJE0FK5MOTICRVE3LFYS450LHFG2RDNBC3EF";
	private String API_PARAM_CLIENT_SECRET = "&client_secret=WN4OSPJESZ0KVSQE3Q0KCAVSW1ZC2VZA1IDA515T4LOIY5EM";

	private Handler handler;
	private HandlerThread handlerThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		handlerThread = new HandlerThread("network-thread");
		handlerThread.start();

		handler = new Handler(handlerThread.getLooper());

		handler.post(new Runnable() {

			public void run() {
				try {
					fetchVenues();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		handlerThread.quit();
	}

	private void fetchVenues() throws IOException, JSONException {
		String url = API_URL_ENDPOINT + API_URL_METHOD_SEARCH;
		url += API_PARAM_VERSION;
		url += "&ll=" + "48.1499132,16.3223658" + "&llAcc=0";
		url += "&categoryId=4d4b7105d754a06374d81259";
		url += "&radius=1000";
		url += "&intent=browse";
		url += API_PARAM_CLIENT_ID;
		url += API_PARAM_CLIENT_SECRET;

		JSONObject venuesJsonObject = fetchJson(url);

		JSONArray venuesArray = venuesJsonObject.getJSONObject("response")
				.getJSONArray("venues");
		for (int i = 0; i < venuesArray.length(); i++) {
			JSONObject venueObject = venuesArray.getJSONObject(i);
			String venueName = venueObject.getString("name");
			String venueId = venueObject.getString("id");

			int venueDistance = venueObject.getJSONObject("location").getInt(
					"distance");

			fetchPhotos(venueId);
		}
	}

	private void fetchPhotos(String venueId) throws IOException, JSONException {
		String url = API_URL_ENDPOINT;
		url += String.format(API_URL_METHOD_PHOTOS, venueId);
		url += API_PARAM_VERSION;
		url += API_PARAM_CLIENT_ID;
		url += API_PARAM_CLIENT_SECRET;

		JSONObject photosJsonObject = fetchJson(url);

		JSONArray photosArray = photosJsonObject.getJSONObject("response")
				.getJSONObject("photos").getJSONArray("items");
		for (int i = 0; i < photosArray.length(); i++) {
			JSONObject photoObject = photosArray.getJSONObject(i);
			String photoUrl = photoObject.getString("prefix")
					+ photoObject.getString("width") + "x"
					+ photoObject.getString("height")
					+ photoObject.getString("suffix");
			photoUrl = URLDecoder.decode(photoUrl, "UTF-8");

			Log.e("smn", photoUrl);
		}
	}

	private JSONObject fetchJson(String url) throws IOException, JSONException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url)
				.openConnection();
		InputStreamReader reader = new InputStreamReader(
				connection.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(reader);

		StringBuilder jsonBuilder = new StringBuilder();
		for (String s = bufferedReader.readLine(); s != null; s = bufferedReader
				.readLine()) {
			jsonBuilder.append(s);
		}

		JSONObject jsonObject = new JSONObject(jsonBuilder.toString());

		bufferedReader.close();
		reader.close();
		connection.disconnect();

		return jsonObject;
	}
}
