package at.tomtasche.nomstagram.engine;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import org.json.JSONObject;

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
            HTTPRequest camfindRequestRequest = createCamfindRequestRequest(photoUrl);
            HTTPResponse camfindRequestResponse = urlFetchService.fetch(camfindRequestRequest);

            logger.log(Level.FINE, new String(camfindRequestResponse.getContent(), Charset.forName("UTF-8")));
            String token = getStringFromJsonResult(camfindRequestResponse, "token");

            HTTPRequest camfindResponseRequest = createCamfindResponseRequest(token);
            HTTPResponse camfindResponseResponse = urlFetchService.fetch(camfindResponseRequest);

            logger.log(Level.FINE, new String(camfindResponseResponse.getContent(), Charset.forName("UTF-8")));
            String food = getStringFromJsonResult(camfindResponseResponse, "name");

            memcacheService.put(photoUrl, food);

            if (isTasty(food)) {
                // return the first photoUrl that looks like food
                return photoUrl;
            }
        }

        return null;
    }

    private HTTPRequest createCamfindRequestRequest(String photoUrl) throws MalformedURLException, UnsupportedEncodingException {
        HTTPRequest httpRequest = new HTTPRequest(new URL("https://camfind.p.mashape.com/image_requests"), HTTPMethod.POST);
        httpRequest.addHeader(new HTTPHeader("X-Mashape-Key", "uRNghWbADRmshyiXU2Yq1Ly4388lp1OpJ8djsn37Dj1pUWITVd"));

        String body = "image_request[locale]=en_US&" + "image_request[remote_image_url]=" + photoUrl;
        body = URLEncoder.encode(body, "UTF-8");

        httpRequest.setPayload(body.getBytes("UTF-8"));

        return httpRequest;
    }

    private HTTPRequest createCamfindResponseRequest(String token) throws MalformedURLException, UnsupportedEncodingException {
        HTTPRequest httpRequest = new HTTPRequest(new URL("https://camfind.p.mashape.com/image_responses/" + token), HTTPMethod.GET);
        httpRequest.addHeader(new HTTPHeader("X-Mashape-Key", "uRNghWbADRmshyiXU2Yq1Ly4388lp1OpJ8djsn37Dj1pUWITVd"));

        return httpRequest;
    }

    private String getStringFromJsonResult(HTTPResponse response, String key) {
        String result = new String(response.getContent(), Charset.forName("UTF-8"));

        JSONObject jsonObject = new JSONObject(result);
        return jsonObject.getString(key);
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
