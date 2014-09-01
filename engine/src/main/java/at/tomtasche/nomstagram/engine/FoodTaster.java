package at.tomtasche.nomstagram.engine;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tom on 30.07.14.
 */
public class FoodTaster {

    private final URLFetchService urlFetchService;

    private final AsyncMemcacheService memcacheService;

    private final Logger logger;

    private final List<String> foodKeywords;

    public FoodTaster() {
        urlFetchService = URLFetchServiceFactory.getURLFetchService();

        memcacheService = MemcacheServiceFactory.getAsyncMemcacheService();

        logger = Logger.getLogger("FoodTaster");

        foodKeywords = new LinkedList<String>();
    }

    public void initialize() {
        AsyncDatastoreService datastoreService = DatastoreServiceFactory.getAsyncDatastoreService();

        Iterable<Entity> allKeywords = FoodHelper.getAllKeywords(datastoreService);
        for (Entity entity : allKeywords) {
            foodKeywords.add((String) entity.getProperty("name"));
        }
    }

    public String findTasty(List<String> photoUrls) throws IOException {
        for (String photoUrl : photoUrls) {
            HTTPRequest tastyRequest = createTastyRequest(photoUrl);
            HTTPResponse tastyResponse = urlFetchService.fetch(tastyRequest);

            String food = getStringFromResponse(tastyResponse);

            memcacheService.put(photoUrl, food);

            logger.log(Level.FINE, "found " + food + " for photo " + photoUrl);

            if (isTasty(food)) {
                // return the first photoUrl that looks like food
                return photoUrl;
            }
        }

        return null;
    }

    private HTTPRequest createTastyRequest(String photoUrl) throws MalformedURLException, UnsupportedEncodingException {
        photoUrl = URLEncoder.encode(photoUrl, "UTF-8");

        HTTPRequest httpRequest = new HTTPRequest(new URL("http://tastyornot.tomtasche.at:8080/?photoUrl=" + photoUrl), HTTPMethod.GET);
        return httpRequest;
    }

    private String getStringFromResponse(HTTPResponse response) {
        String result = new String(response.getContent(), Charset.forName("UTF-8"));

        return result;
    }

    private boolean isTasty(String food) {
        for (String foodKeyword : foodKeywords) {
            if (food.contains(foodKeyword)) {
                return true;
            }
        }

        return false;
    }
}
