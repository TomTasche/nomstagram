package at.tomtasche.nomstagram.engine;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(name = "foursquareApi", version = "v1", namespace = @ApiNamespace(ownerDomain = "engine.nomstagram.tomtasche.at", ownerName = "engine.nomstagram.tomtasche.at", packagePath=""))
public class FoursquareEndpoint {

    private static final String API_URL_ENDPOINT = "https://api.foursquare.com/v2/";
    private static final String API_URL_METHOD_SEARCH = "venues/search";
    private static final String API_URL_METHOD_PHOTOS = "venues/%s/photos";

    private static final String API_PARAM_VERSION = "?v=20140712";
    private static final String API_PARAM_CLIENT_ID = "&client_id=H4ZMXXNEL2HFTJE0FK5MOTICRVE3LFYS450LHFG2RDNBC3EF";
    private static final String API_PARAM_CLIENT_SECRET = "&client_secret=WN4OSPJESZ0KVSQE3Q0KCAVSW1ZC2VZA1IDA515T4LOIY5EM";

    private final FoodTaster foodTaster;

    public FoursquareEndpoint() {
        foodTaster = new FoodTaster();

        foodTaster.initialize();
    }

    @ApiMethod(name = "venues")
    public List<Venue> getVenues(@Named("lat") double lat, @Named("lon") double lon, @Named("radius") int radius) {
        try {
            return fetchVenues(lat, lon, radius);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "could not fetch venues", e);
        }

        return Collections.EMPTY_LIST;
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

            String foodUrl = foodTaster.findTasty(venuePhotos);
            if (foodUrl != null) {
                venuePhotos.add(0, foodUrl);
            }

            Venue venue = new Venue(venueId, "foursquare");
            venue.setName(venueName);
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
                    + photoObject.getInt("width") + "x"
                    + photoObject.getInt("height")
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
            Logger.getGlobal().log(Level.WARNING, "could not fetch JSON", e);
        } finally {
            connection.disconnect();
        }

        return null;
    }
}