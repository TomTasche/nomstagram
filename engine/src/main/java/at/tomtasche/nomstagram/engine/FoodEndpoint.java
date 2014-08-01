package at.tomtasche.nomstagram.engine;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(name = "foodApi", version = "v1", namespace = @ApiNamespace(ownerDomain = "engine.nomstagram.tomtasche.at", ownerName = "engine.nomstagram.tomtasche.at", packagePath=""))
public class FoodEndpoint {

    private final AsyncDatastoreService datastoreService;

    public FoodEndpoint() {
        datastoreService = DatastoreServiceFactory.getAsyncDatastoreService();
    }

    @ApiMethod(name = "keywords")
    public void addKeywords(@Named("keywords") String newKeywords) {
        Iterable<Entity> keywords = FoodHelper.getAllKeywords(datastoreService);

        String[] splitKeywords = newKeywords.split(",");
        for (int i = 0; i < splitKeywords.length; i++) {
            Entity keywordEntity = new Entity("keyword");
            keywordEntity.setProperty("name", splitKeywords[i]);

            datastoreService.put(keywordEntity);
        }
    }
}