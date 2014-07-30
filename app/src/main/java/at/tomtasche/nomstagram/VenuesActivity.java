package at.tomtasche.nomstagram;

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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;
import java.util.List;

import at.tomtasche.nomstagram.engine.foursquareApi.FoursquareApi;
import at.tomtasche.nomstagram.engine.foursquareApi.model.Venue;

public class VenuesActivity extends FragmentActivity implements VenueProvider, LocationListener {

    private static final int SEARCH_RADIUS = 1000;

    private Handler mainHandler;
    private Handler networkHandler;
    private HandlerThread networkHandlerThread;

    private FoursquareApi foursquare;

    private LocationManager locationManager;
    private final Object locationLock;
    private Location lastLocation = null;

    private VenueLoadCallback pendingVenuesRequest = null;
    private int pendingVenuesRequestOffset = 0;

    public VenuesActivity() {
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

        FoursquareApi.Builder builder = new FoursquareApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null)
                // options for running against local devappserver
                // - 10.0.2.2 is localhost's IP address in Android emulator
                .setRootUrl("http://192.168.0.116:8080/_ah/api/")
                .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                    @Override
                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                        // - turn off compression when running against local devappserver
                        abstractGoogleClientRequest.setDisableGZipContent(true);
                    }
                });

        // TODO: AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);

        foursquare = builder.build();
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

                    FoursquareApi.Venues venues = foursquare.venues(lat, lon, radius);
                    final List<Venue> venuesList = venues.execute().getItems();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onVenuesLoaded(venuesList);
                        }
                    });
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    finish();
                }
            }
        });
    }
}
