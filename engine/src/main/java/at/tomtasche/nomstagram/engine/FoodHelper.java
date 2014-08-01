package at.tomtasche.nomstagram.engine;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

/**
 * Created by tom on 30.07.14.
 */
public class FoodHelper {

    public static Iterable<Entity> getAllKeywords(AsyncDatastoreService datastoreService) {
        return datastoreService.prepare(new Query("keyword")).asIterable();
    }
}
