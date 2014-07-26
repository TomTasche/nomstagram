package at.tomtasche.nomstagram;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public class VenueActivity extends FragmentActivity implements VenueProvider, LocationListener {

    public static final String FOURSQUARE_URL_BASE = "https://foursquare.com/";
    public static final String FOURSQUARE_PATH_VENUE = "v/";

    private static final String API_URL_ENDPOINT = "https://api.foursquare.com/v2/";
    private static final String API_URL_METHOD_SEARCH = "venues/search";
    private static final String API_URL_METHOD_PHOTOS = "venues/%s/photos";

    private static final String API_PARAM_VERSION = "?v=20140712";
    private static final String API_PARAM_CLIENT_ID = "&client_id=H4ZMXXNEL2HFTJE0FK5MOTICRVE3LFYS450LHFG2RDNBC3EF";
    private static final String API_PARAM_CLIENT_SECRET = "&client_secret=WN4OSPJESZ0KVSQE3Q0KCAVSW1ZC2VZA1IDA515T4LOIY5EM";

    private static final int SEARCH_RADIUS = 1000;

    private Handler mainHandler;
	private Handler networkHandler;
	private HandlerThread networkHandlerThread;

    private LocationManager locationManager;
    private final Object locationLock;
    private Location lastLocation = null;

    private VenueLoadCallback pendingVenuesRequest = null;
    private int pendingVenuesRequestOffset = 0;

    public VenueActivity() {
        locationLock = new Object();
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestSingleUpdate(criteria, this, getMainLooper());

        final FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            final VenueGridFragment fragment = new VenueGridFragment();
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }

		networkHandlerThread = new HandlerThread("network-thread");
		networkHandlerThread.start();

		networkHandler = new Handler(networkHandlerThread.getLooper());

        mainHandler = new Handler();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();

		networkHandlerThread.quit();
	}

    @Override
    public void onLocationChanged(Location location) {
        synchronized (locationLock) {
            lastLocation = location;
        }

        locationManager.removeUpdates(this);

        if (pendingVenuesRequest != null) {
            loadVenues(pendingVenuesRequest, pendingVenuesRequestOffset);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    public void loadVenues(VenueLoadCallback callback) {
        loadVenues(callback, 0);
    }

    @Override
    public void loadVenues(final VenueLoadCallback callback, final int offset) {
        synchronized (locationLock) {
            if (lastLocation == null) {
                pendingVenuesRequest = callback;
                pendingVenuesRequestOffset = offset;

                return;
            }
        }

        networkHandler.post(new Runnable() {

            public void run() {
                try {
                    double lat = lastLocation.getLatitude();
                    double lon = lastLocation.getLongitude();

                    int radius = SEARCH_RADIUS;
                    radius *= offset + 1;

                    final List<Venue> venues = fetchVenues(lat, lon, radius);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onVenuesLoaded(venues);
                        }
                    });
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

    private List<Venue> fetchVenues(double lat, double lon, int radius) throws IOException, JSONException {
		String url = API_URL_ENDPOINT + API_URL_METHOD_SEARCH;
		url += API_PARAM_VERSION;
		url += "&ll=" + lat + "," + lon + "&llAcc=0";
		url += "&categoryId=4d4b7105d754a06374d81259";
		url += "&radius=" + radius;
		url += "&intent=browse";
		url += API_PARAM_CLIENT_ID;
		url += API_PARAM_CLIENT_SECRET;

        List<Venue> venues = new ArrayList<Venue>();

		JSONObject venuesJsonObject = fetchJson(url);

		JSONArray venuesArray = venuesJsonObject.getJSONObject("response")
				.getJSONArray("venues");
		for (int i = 0; i < venuesArray.length(); i++) {
            JSONObject venueObject = venuesArray.getJSONObject(i);
			String venueName = venueObject.getString("name");
			String venueId = venueObject.getString("id");

			int venueDistance = venueObject.getJSONObject("location").getInt(
					"distance");

			List<String> venuePhotos = fetchPhotos(venueId);
            if (venuePhotos.size() <= 0) {
                // it doesn't make sense right now to include restaurants without photos in this app

                continue;
            }

            Venue venue = new Venue(venueId);
            venue.addPhotoUrls(venuePhotos);

            venues.add(venue);
        }

        return venues;
	}

	private List<String> fetchPhotos(String venueId) throws IOException, JSONException {
		String url = API_URL_ENDPOINT;
		url += String.format(API_URL_METHOD_PHOTOS, venueId);
		url += API_PARAM_VERSION;
		url += API_PARAM_CLIENT_ID;
		url += API_PARAM_CLIENT_SECRET;

        List<String> venuePhotos = new ArrayList<String>();

		JSONObject photosJsonObject = fetchJson(url);
        if (photosJsonObject == null) {
            return venuePhotos;
        }

		JSONArray photosArray = photosJsonObject.getJSONObject("response")
				.getJSONObject("photos").getJSONArray("items");
		for (int i = 0; i < photosArray.length(); i++) {
			JSONObject photoObject = photosArray.getJSONObject(i);
			String photoUrl = photoObject.getString("prefix")
					+ photoObject.getString("width") + "x"
					+ photoObject.getString("height")
					+ photoObject.getString("suffix");
			photoUrl = URLDecoder.decode(photoUrl, "UTF-8");

            venuePhotos.add(photoUrl);
		}

        return venuePhotos;
	}

	private JSONObject fetchJson(String url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url)
                .openConnection();
        try {
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

            return jsonObject;
        } catch (FileNotFoundException e) {
            Log.w("nomstagram", e);
        } finally {
            connection.disconnect();
        }

        return null;
    }
}
